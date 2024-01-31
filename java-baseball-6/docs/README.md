# 미션 - 숫자 야구

## 🚀 기능 요구 사항
- [ ] 처음 시작 시 세자리를 **입력받는다.**
  - [ ] 입력 받는 숫자는 서로 다른 숫자 여야 한다. 
- [ ] 숫자를 **입력 한다.**
  - [ ] 잘못 된 방식의 입력 경우 IllegalArguentException (숫자여야 함)
- [ ] 입력 받은 숫자를 확인하고 결과를 알려준다.
  - 상대방(컴퓨터)의 수가 425일 때  
    123을 제시한 경우 : 1스트라이크  
    456을 제시한 경우 : 1볼 1스트라이크  
    789를 제시한 경우 : 낫싱  
  - 같은 자리에 같은 숫자면 **스트라이크**  
    숫자는 있지만 자리가 다를 때 **볼**    
    같은 숫자가 아예 없을 경우 **낫싱**
- [ ] 3스트라이크, 모든 숫자를 맞출 때 까지 반복한다.
- [ ] 게임이 끝나고 다시 할 건지 묻는다.

## Convention

### Java Sytle Convention
- 블럭 들여쓰기: +4 스페이스
새 블록 또는 블록과 유사한 구조(block-like construct)가 열릴 때마다 들여쓰기가 네 칸씩 증가합니다. 블록이 끝나면 들여쓰기는 이전 들여쓰기 단계로 돌아갑니다. 들여쓰기 단계는 블록 전체의 코드와 주석 모두에 적용됩니다.
- 열 제한: 120
Java 코드의 열 제한은 120자입니다. "문자"는 유니코드 코드 포인트를 의미합니다.
- 들여쓰기 지속은 최소 +8 스페이스
줄 바꿈 시 그 다음 줄은 원래 줄에서 +8 이상 들여씁니다.

### Git Commit Convention
- feat : 기능 추가
- fix : 버그 수정
- docs : 문서 수정
- test : 테스트 코드 추가
- refactor : 코드 리팩토링
- style : 코드 의미에 영향을 주지 않는 변경사항
- chore : 빌드 부분 혹은 패키지 매니저 수정사항
- cicd : CI/CD 관련 설정