# Smartthings Device Type Handler

eZEX Power Meter v0.1  (한전 계산기 적용)

*[주택 3KW 이하 저전압기준]
   
[기타계절] 1~6 , 9~12
   1단계 : 200kWh × 93.3원 
   2단계 : 200kWh ~ 400KwH × 187.9원
   3단계 : 400kWh × 280.6원

[하계] 7~8
   1단계 : 300kWh × 93.3원 
   2단계 : 301kWh ~ 450kWh × 187.9원
   3단계 : 450kWh × 280.6원
   공통 누진4단계 : 1000kWh 이상 x 709.5원

[기본료]
   누진1단계 기본료 910원 ~200K
   누진2단계 기본료 1600원 201K~400
   누진3단계 기본료 7300원 401K~

[총계산] 
   전기세 = (기본료+전기세)
   부가세 = 전기세 x 0.1 
   전력산업기반기금 = 전기세 x 0.037 
   총액 = 전기세 + 부가세 + 전력산업기반기금
   [6~7] or [8~9] 월 경우

하계/기타계절 요금계산법 = 누진단계x계절일수/30 + 누진단계x하계일수/30


Zemismart Button
* New app compatibility(Automation, Log, UI)

   [Button 1]     
   inClusters: "0000, 0001, 0006" 
   outClusters: "0019" 
   manufacturer: "_TYZB02_keyjqthh" 
   model: "TS0041"
         
   [Button 2]     
   inClusters: "0000, 0001, 0006"
   outClusters: "0019" 
   manufacturer: "_TYZB02_keyjhapk"
   model: "TS0042"
        
   [Button 3]     
   inClusters: "0000, 0001, 0006"
   outClusters: "0019"
   manufacturer: "_TZ3400_key8kk7r"
   model: "TS0043"
        
