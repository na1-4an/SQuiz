// MainPage.tsx
import React, { useState } from "react";
import { CenteredContainer, FormContainer } from "../widgets/styles";
import FileUpload from "../components/FileUpload";
import styled from "styled-components";
import { TextField, Autocomplete, Button } from "@mui/material";
import Header from "../components/Header";

export const FormContainer2 = styled.div`
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 500px;
  height: 400px;
  background-color: white;
  padding: 50px;
  border-radius: 24px;
  align-items: center; /* 텍스트를 수직 방향으로 가운데 정렬 */
  justify-content: center;
  text-align: center;
`;

const Label = styled.div`
  display: flex;
  align-items: center; /* 텍스트를 수직 방향으로 가운데 정렬 */
  font-weight: bold;
  margin-bottom: 10px;
`;

const LabelSmall = styled.div`
  display: flex;
  align-items: center; /* 텍스트를 수직 방향으로 가운데 정렬 */
  min-width: 100px; /* 필요하다면 최소 너비 지정 */
  color: gray;
  font-size: 14px;
`;

const subjectStyle = {};

const MainPage: React.FC = () => {
  const jwtToken = localStorage.getItem("authToken") || "";
  const [isSubject, setIsSubject] = useState<boolean>(false);

  const [subjects, setSubjects] = useState<{ label: string; value: string }[]>([
    { label: "운영체제", value: "OPERATING_SYSTEM" },
    { label: "컴퓨터통신", value: "COMPUTER_COMMUNICATION" },
    { label: "객체지향프로그래밍", value: "OBJECT_ORIENTED_PROGRAMMING" },
    { label: "C프로그래밍", value: "C_LANGUAGE" },
  ]);
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);

  const handleConfirmClick = () => {
    if (selectedSubject) {
      setIsSubject(true);
    }
  };

  return jwtToken && isSubject ? (
    <CenteredContainer>
      <Header />
      <FormContainer>
        <FileUpload token={jwtToken} selectedSubject={selectedSubject} />
      </FormContainer>
    </CenteredContainer>
  ) : (
    <CenteredContainer>
      <Header />
      <FormContainer2>
        <LabelSmall>퀴즈/요약본을 만들고 싶은</LabelSmall>
        <Label>과목을 선택하세요.</Label>

        <Autocomplete
          style={subjectStyle}
          options={subjects}
          getOptionLabel={(option) => option.label}
          renderInput={(params) => (
            <TextField
              {...params}
              InputProps={{
                ...params.InputProps,
                style: {
                  paddingTop: "3px", // 수직 정렬을 위해 상단 패딩 조정
                  borderRadius: "20px",
                  width: "30vh",
                },
              }}
            />
          )}
          sx={{
            "& .MuiInputBase-root": {
              height: "40px",
            },
          }}
          value={
            subjects.find((subject) => subject.value === selectedSubject) ||
            null
          }
          onChange={(
            event: any,
            newValue: { label: string; value: string } | null
          ) => setSelectedSubject(newValue?.value || null)}
        />
        <Button
          variant="contained"
          size="large"
          sx={{
            color: "white",
            background: "#ffc450",
            borderRadius: "16px",
            width: "25vh",
            marginTop: "20vh",
          }}
          onClick={handleConfirmClick}
        >
          확인
        </Button>
      </FormContainer2>
    </CenteredContainer>
  );
};

export default MainPage;