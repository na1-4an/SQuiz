package com.jmdm.squiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmdm.squiz.domain.*;
import com.jmdm.squiz.dto.*;
import com.jmdm.squiz.enums.KC;
import com.jmdm.squiz.enums.QuizType;
import com.jmdm.squiz.enums.SubjectType;
import com.jmdm.squiz.exception.ErrorCode;
import com.jmdm.squiz.exception.SuccessCode;
import com.jmdm.squiz.exception.model.NotFoundQuizException;
import com.jmdm.squiz.repository.*;
import com.jmdm.squiz.utils.ApiResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class QuizCheckService {
    private final QuizRepository quizRepository;
    private final ProblemRepository problemRepository;
    private final DktPerSubjectRepository dktPerSubjectRepository;
    private final DktListRepository dktListRepository;
    private final MemberRepository memberRepository;

    public QuizCheckResponse checkQuiz(String memberId, QuizCheckRequest request) throws IOException {
        // quiz 불러오기
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new NotFoundQuizException(ErrorCode.QUIZ_NOT_FOUND, ErrorCode.QUIZ_NOT_FOUND.getMessage()));

        // 해당 퀴즈의 문제들 불러오고 문제 번호 순으로 정렬
        List<Problem> problems = quiz.getProblems();
        problems.sort(Comparator.comparingInt(Problem::getProblemNo));

        // 사용자가 푼 문제 목록 불러오고 문제 번호 순으로 정렬
        List<CheckProblemDTO> checkProblemDTOS = request.getProblems();
        checkProblemDTOS.sort(Comparator.comparingInt(CheckProblemDTO::getProblemNo));

        // 문제에 사용자가 체크한 답을 저장
        IntStream.range(0, quiz.getProblemNum() - 1).forEach(i -> {
            Problem problem = problems.get(i);
            CheckProblemDTO checkProblemDTO = checkProblemDTOS.get(i);
            problem.setCheckedAnswer(checkProblemDTO.getCheckedAnswer(), checkProblemDTO.getCheckedBlanks());

            boolean isCorrect = (quiz.getQuizType() == QuizType.BLANK)
                    ? problem.getCheckedBlanks().equalsBlanks(problem.getBlanks())
                    : problem.getCheckedAnswer().equals(problem.getAnswer());
            if (isCorrect) {
                problem.setCorrect(1);
            } else {
                problem.setCorrect(0);
            }
        });

        problemRepository.saveAll(problems);
        List<KcDTO> kcDTOS = new ArrayList<>();
        for (Problem problem : problems) {
            KcDTO dto = new KcDTO();
            dto.setKcId(problem.getKcId());
            dto.setCorrect(problem.getCorrect());
            kcDTOS.add(dto);
        }
        AiQuizCheckResponse AiResponse = postAiAndGetDkt(memberId, quiz.getSubject(), kcDTOS);



        return gradeQuiz(quiz, problems);
    }

    private void saveDkt(AiQuizCheckResponse response) {
        Member member = memberRepository.findByMemberId(response.getMemberId());
        DktPerSubject dktPerSubject = DktPerSubject.builder()
                .member(member)
                .subjectType(response.getSubjectType())
                .build();
        dktPerSubjectRepository.save(dktPerSubject);
        for (Dkt dkt : response.getDkt()) {
            DktList dktList = DktList.builder()
                    .kcId(dkt.getKcId())
                    .predict(dkt.getPredict())
                    .dktPerSubject(dktPerSubject)
                    .build();
            dktListRepository.save(dktList);
        }
    }


    private QuizCheckResponse gradeQuiz(Quiz quiz, List<Problem> problems) {
        int correctNum = 0;
        Map<String, CorrectPerKcDTO> correctPerKcMap = new HashMap<>();

        for (Problem problem : problems) {
            String targetKcName = KC.fromId(problem.getKcId());
            System.out.println("targetKcName = " + targetKcName);
            CorrectPerKcDTO dto = correctPerKcMap.computeIfAbsent(targetKcName, k -> {
                CorrectPerKcDTO newDto = new CorrectPerKcDTO();
                newDto.setKcName(k);
                newDto.setKcProblemNum(0);
                return newDto;
            });
            System.out.println("dto = " + dto);

            if (problem.getCorrect() == 1) {
                correctNum++;
                dto.correctProblem();
            } else {
                dto.wrongProblem();
            }
        }
        List<CorrectPerKcDTO> correctPerKcDTOS = new ArrayList<>(correctPerKcMap.values());
        quiz.setCorrectNum(correctNum);
        quizRepository.save(quiz);
        return makeResponse(quiz, correctNum, correctPerKcDTOS);
    }

    private QuizCheckResponse makeResponse(Quiz quiz, int correctNum, List<CorrectPerKcDTO> correctPerKcDTOS) {
        QuizCheckResponse response = new QuizCheckResponse();
        response.setQuizId(quiz.getId());
        response.setQuizName(quiz.getQuizName());
        response.setProblemNum(quiz.getProblemNum());
        response.setCorrectNum(correctNum);
        response.setCorrectPerKcDTOS(correctPerKcDTOS);
        return response;
    }

    private AiQuizCheckResponse postAiAndGetDkt(String memberId, SubjectType subjectType, List<KcDTO> interactions) throws IOException {
        // post 요청할 ai 서버 url
        String aiServerUrl = "http://192.168.0.166:8000/api/v1/dkt";
        // 요청 header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 body 설정
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("memberId", memberId);
        body.add("subject", subjectType);
        body.add("interactions", interactions);

        // post 요청
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MultiValueMap<String, Object>> aiRequest = new HttpEntity<>(body, headers);
        ResponseEntity<String> aiResponse = restTemplate.postForEntity(aiServerUrl, aiRequest, String.class);

        // response 처리
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(aiResponse.getBody(), AiQuizCheckResponse.class);
    }
}