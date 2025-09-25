User
심다은 - user ,cart 
Product
권아연 - book, review, search
Order
김재훈 - order, payment

깃 정리 

  현재 개인 브랜치에서 작업중인 내용 작성완료시 개인 브랜치에 먼저 push함 master에선 ㄴㄴ
  ex)git commit -m"수정한 내용 뭔지 작성(이후 확인용)" -> git push origin (각자 브랜치명)

  이후 원격 or 로컬에서 pull request 보냄 그러면 다른사람이 확인하고 merge를 함(현재 승인자가 따로없어서 셀프로도 가능해요)
  그러면 마스터 브랜치가 내 브랜치 변경사항을 반영하게 됨
  
  그러면 이제 충돌이 적게 나게 하려면(작업이 길어질수록 여러 코드 만지다보니 충돌 가능성이 올라감) 
  개인 브랜치 로컬에서 git pull origin master(자동으로 머지됩니다)를 해줘야됨 
  여기서 만약 충돌 발생시 어디거를 적용할건지 물어볼어보면 골라줌(도메인 별로 분리되 있어서 아마 적게 날거임**불확실)
  그 이후 작업하던거 마저하고 개인 브랜치에 push ㄱㄱ


  
  
      
