/**
 *  EZEX POWER METER test V0.2
 *
 *  Copyright 2020 YSB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "Ezex Power Meter", namespace: "SangBoy", author: "YooSangBeom", mnmn: "SmartThings", ocfDeviceType: "x.com.st.d.energymeter", vid: "SmartThings-smartthings-Aeon_Home_Energy_Meter") {
        capability "Energy Meter"
        capability "Power Meter"
        capability "Refresh"
        capability "Health Check"
        capability "Sensor"
        capability "Configuration"
        
        attribute "kwhTotal", "number"		// this is value reported by the switch since joining the hub.  See change log above for details.
        attribute "resetTotal", "number"	           // used to calculate accumulated kWh after a reset by the user.  See change log above for details.
        command "reset"

        fingerprint profileId: "0104", deviceId:"0053", inClusters: "0000, 0003, 0004, 0B04, 0702", outClusters: "0019", manufacturer: "", model: "E240-KR080Z0-HA", deviceJoinName: "EZEX Energy Monitor(CT)"
        
    }
	simulator 
    {
	}
    
	preferences 
    {       
        input name: "Meter_reading_date", title:"검침일" , type: "text", required: true, defaultValue: 7   
	}

    // tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"power", type: "generic", width: 5, height: 5)
        {
            tileAttribute("device.power", key: "PRIMARY_CONTROL") 
            {
                attributeState("default", label:'${currentValue} Watt')
            }
            tileAttribute("device.powerConsumption_step", key: "SECONDARY_CONTROL") 
            {
                attributeState("default", label:'${currentValue} 단계 적용 중! 아껴써요!', icon: "st.Appliances.appliances17")
            }
        }
        standardTile("energy", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 1) 
        {
            state "default", label:'이번달 누적 : ${currentValue} kWh'
        }    
        standardTile("powerConsumption", "device.powerConsumption", inactiveLabel: false, decoration: "flat", width: 2, height: 1) 
        {
            state "default", label:'기기누적 : ${currentValue} kWh'
        }     
        standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 1) 
        {
            state "default", label:'reset kWh', action:"reset", icon: "st.secondary.refresh-icon"
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 1) 
        {
            state "default", label:'', action:"refresh.refresh", icon: "st.secondary.refresh-icon"
        }
		valueTile("Meter_reading_date", "device.Meter_reading_date", width: 2, height: 1) 
        {
            state "val", label: '검침일 : ${currentValue}일'
        }         
 		valueTile("Season", "device.Season", width: 2, height: 1) 
        {
            state "val", label: '시즌 : ${currentValue}'
        }      
        valueTile("Summer_season", "device.Summer_season", width: 2, height: 1) 
        {
            state "val", label: '하계시즌일 : ${currentValue}일'
        }      
        valueTile("Etc_season", "device.Etc_season", width: 2, height: 1) 
        {
            state "val", label: '계절시즌일 : ${currentValue}일'
        }  
        valueTile("Electric_charges", "device.Electric_charges", width: 2, height: 1) 
        {
            state "val", label: '현재요금 : ${currentValue}원'
        }          


        main (["power",  "powerConsumption_step"])
        details(["power", "energy", "powerConsumption", "Meter_reading_date"
        		, "Season", "Summer_season", "Etc_season", "Electric_charges"
        		, "reset", "refresh"])
         
    }
}


def initialize() 
{ 
  log.debug "call initialize" 
}

def handlerMethod() 
{
   log.debug "Event run this month"
   reset() 
}

def reset() 
{
    log.debug "Resetting kWh..."
    sendEvent(name: "resetTotal", value: device.currentState('kwhTotal')?.doubleValue, unit: "kWh")
    sendEvent(name: "energy", value: 0, unit: "kWh")
    log.debug "Event registration that runs once a month. - YSB"
    schedule("0 0 0 ${Meter_reading_date.value} 1/1 ? *", handlerMethod) 
    //설정된 매월 검침일 00:00 누적전력 초기화 호출 ,cronmaker 참조
       
}

def parse(String description) 
{
    log.debug "description is $description"
    def descMap = zigbee.parseDescriptionAsMap(description)    
    def event = zigbee.getEvent(description)

    if (event) 
    {
        log.info event
        if (event.name == "power") 
        {
            if (device.currentState('resetTotal')?.doubleValue == null) 
            {
    			sendEvent(name: "resetTotal", value: 0, unit: "kWh")
            }
            else if (descMap.clusterInt == 0x0702 && descMap.attrInt == 0x0400) 
            {
            	event.value = Math.round(event.value/1000)
            	event.unit = "W"
                sendEvent(name: "power", value : Math.round(event.value), unit: "W")            
            }
            else if (descMap.clusterInt == 0x0b04 && descMap.attrInt == 0x050b) 
            {
               	event.value = Math.round(event.value/10)
            	event.unit = "W"
            }
 			else if (descMap.clusterInt == 0x0702 && descMap.attrInt == 0x0000) 
            {
                log.debug "Energy_log $descMap.clusterInt" 
                event.name = "powerConsumption"
                event.value = Math.round(event.value/1000000)
                event.unit = "kWh"
                log.info "event outer:$event"
                sendEvent(event)

                if (device.currentState('resetTotal')?.doubleValue == null) 
                {
                   sendEvent(name: "resetTotal", value: 0, unit: "kWh")
                }
                else
                {
                   def value = Math.round(event.value) - device.currentState('resetTotal')?.doubleValue
                   sendEvent(name: "energy", value: Math.round(value), unit: "kWh")
                   sendEvent(name: "kwhTotal", value:  Math.round(event.value) , unit: "kWh", displayed: false)               
                }         
            }            
        } 
        else if (event.name == "energy") 
        {
            log.debug "Energy_log $descMap.clusterInt" 
            event.value = Math.round(event.value/1000000)
            event.unit = "kWh"
            log.info "event outer:$event"
            //sendEvent(event)           
            if (device.currentState('resetTotal')?.doubleValue == null) 
            {
               sendEvent(name: "resetTotal", value: 0, unit: "kWh")
            }
            else
            {
               def value = Math.round(event.value) - device.currentState('resetTotal')?.doubleValue
               sendEvent(name: "energy", value: Math.round(value), unit: "kWh")
               sendEvent(name: "kwhTotal", value: Math.round(value), unit: "kWh", displayed: false)               
            }
          
        }

    } 
    else  
    {
        List result = []
        //def descMap = zigbee.parseDescriptionAsMap(description)
        log.debug "Desc Map: $descMap"
                
        List attrData = [[clusterInt: descMap.clusterInt ,attrInt: descMap.attrInt, value: descMap.value]]
        descMap.additionalAttrs.each 
        {
            attrData << [clusterInt: descMap.clusterInt, attrInt: it.attrInt, value: it.value]
        }
        attrData.each 
        {
                def map = [:]
                             
                if (it.clusterInt == 0x0702 && it.attrInt == 0x0400) 
                {
                        log.debug "meter"
                        map.name = "power"
                        map.value = Math.round(zigbee.convertHexToInt(it.value)/1000)
                        map.unit = "W"
                }
                if (it.clusterInt == 0x0b04 && it.attrInt == 0x050b) 
                {
                        log.debug "meter"
                        map.name = "power"
                        map.value = Math.round(zigbee.convertHexToInt(it.value)/10)
                        map.unit = "W"
                } 
                
                if (it.clusterInt == 0x0702 && it.attrInt == 0x0000) 
                {
                        log.debug "energy"
                        map.name = "powerConsumption"
                        map.value = Math.round(zigbee.convertHexToInt(it.value)/1000000)
                        map.unit = "kWh"

                        if (device.currentState('resetTotal')?.doubleValue == null) 
                        {
                           sendEvent(name: "resetTotal", value: 0, unit: "kWh")
                        }
                        else
                        {
                           def value = Math.round(zigbee.convertHexToInt(it.value)/1000000) - device.currentState('resetTotal')?.doubleValue
                           sendEvent(name: "energy", value: Math.round(value), unit: "kWh")
                           sendEvent(name: "kwhTotal", value:Math.round(zigbee.convertHexToInt(it.value)/1000000), unit: "kWh", displayed: false)               
                        }                             
                }          
                if (map) 
                {
                        result << sendEvent(map)
                }
                log.debug "Parse returned $map"
        }
        return result      
    }
/*
주택 3KW 이하 저전압기준

[기타계절] 1~6 , 9~12
·1단계 : 200kWh           × 93.3원  
·2단계 : 200kWh ~ 400KwH  × 187.9원 
·3단계 : 339kWh           × 280.6원 

[하계] 7~8
·1단계 : 300kWh             × 93.3원 
·2단계 : 301kWh ~ 450kWh    × 187.9원
·3단계 : 450kWh             × 280.6원 

 공통 누진4단계 : 1000kWh 이상 x 709.5원 

[기본료]
누진1단계 기본료 910원     ~200K
누진2단계 기본료 1600원  201K~400
누진3단계 기본료 7300원  401K~

[총계산] 
전기세           = (기본료+전기세)
부가세           = 전기세 x 0.1  
전력산업기반기금 = 전기세 x 0.037 
총액 = 전기세 + 부가세 + 전력산업기반기금

[6~7] or [8~9] 월 경우
하계/기타계절 요금계산법 = 누진단계x계절일수/30 + 누진단계x하계일수/30
*/

def basic_fare 
def month_energy = device.currentState('energy')?.doubleValue
def this_month = (new Date().format("MM", location.timeZone))


    if(month_energy <=200)
       basic_fare=910
    else if(month_energy <=400)
       basic_fare=1600
    else
       basic_fare=7300
    
    
    
    if( (this_month == '07') || (this_month == '09')) // 6~7 or 8~9
    {
       sendEvent(name: 'Season', value: "계절/하계시즌") 
       sendEvent(name: 'Summer_season', value: Meter_reading_date.value)
       sendEvent(name: 'Etc_season', value: (30 - ${Meter_reading_date.value}))    
       
       def season_etc = Meter_reading_date.value/30
       def season_summmer =  (30 - ${Meter_reading_date.value})/30
       def season_etc_energy = month_energy*season_etc
       def season_summer_energy = month_energy*season_summmer
       
       if(month_energy <= 200)
       {
          def temp_charge = Math.round(basic_fare+month_energy*93.3) 
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  ) 
          sendEvent(name: 'powerConsumption_step', value: "계절/하계 누진1")          
       }
       else if(month_energy <=300)
       {
          def temp_charge = Math.round(               basic_fare + //기본 요금
          						          (200*season_etc*93.3) + //계절 누진1 요금
               ((season_etc_energy - (200*season_etc)) * 187.9) + //계절 누진2 요금
                                   (season_summer_energy*93.3))   //하계 누진1 요금
          
       
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  ) 
          sendEvent(name: 'powerConsumption_step', value: "계절2 / 하계1")         
       }
       else if(month_energy <=400)
       {   
          def temp_charge = Math.round(               basic_fare + //기본 요금
          						          (200*season_etc*93.3) + //계절 누진1 요금
               ((season_etc_energy - (200*season_etc)) * 187.9) + //계절 누진2 요금
                                      (200*season_summmer*93.3) + //하계 누진1 요금
       ((season_summer_energy - (200*season_summmer)) * 187.9))   //하계 누진2 요금
          
          
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  ) 
          sendEvent(name: 'powerConsumption_step', value: "계절2 / 하계2")          
       }
       else if(month_energy <=450)
       {
          def temp_charge = Math.round(               basic_fare + //기본 요금
          						          (200*season_etc*93.3) + //계절 누진1 요금
                                         (200*season_etc*187.9) + //계절 누진2 요금
             ((season_etc_energy - (200*season_etc*2)) * 280.6) + //계절 누진3 요금          
                                      (200*season_summmer*93.3) + //하계 누진1 요금
       ((season_summer_energy - (200*season_summmer)) * 187.9))   //하계 누진2 요금
  
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  ) 
          sendEvent(name: 'powerConsumption_step', value: "계절3 / 하계2")          
       }
       else if(month_energy <=1000)
       {
          def temp_charge = Math.round(               basic_fare + //기본 요금
          						          (200*season_etc*93.3) + //계절 누진1 요금
                                         (200*season_etc*187.9) + //계절 누진2 요금
             ((season_etc_energy - (200*season_etc*2)) * 280.6) + //계절 누진3 요금          
                                      (200*season_summmer*93.3) + //하계 누진1 요금
                                     (200*season_summmer*187.9) + //하계 누진2 요금                                      
       ((season_summer_energy - (200*season_summmer*2)) * 280.6)) //하계 누진3 요금
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  ) 
          sendEvent(name: 'powerConsumption_step', value: "계절3 / 하계3")        
       }
    }
    else if(this_month == '08') // 7~8
    {     
       sendEvent(name: 'Season', value: "하계시즌") 
       sendEvent(name: 'Summer_season', value: 30) 
       sendEvent(name: 'Etc_season', value: 0)        
       
       if(month_energy <= 300)
       {     
          def temp_charge = Math.round(basic_fare+month_energy*93.3)
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 

          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  ) 
          sendEvent(name: 'powerConsumption_step', value: "현재 누진1")     
       }
       else if(month_energy <= 450)
       {
          def temp_charge = Math.round(basic_fare+(300*93.3)+((month_energy-300)*187.9))
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  )      
          sendEvent(name: 'powerConsumption_step', value: "현재 누진2")
       }
       else if(month_energy <= 1000)
       {
          def temp_charge = Math.round(basic_fare+(300*93.3)+(150*187.9)+((month_energy-450)*280.6))
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  )      
          sendEvent(name: 'powerConsumption_step', value: "현재 누진3")       
       }
       else
       {
          def temp_charge = Math.round(basic_fare+(300*93.3)+(150*187.9)+(550*280.6)+((month_energy-1000)*709.5))
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  )      
          sendEvent(name: 'powerConsumption_step', value: "현재 슈퍼누진4")          
       }
    }
    else //1~6 , 9~12
    {
       Etc_season.value = 30
       sendEvent(name: 'Season', value: "계절시즌") 
       sendEvent(name: 'Summer_season', value: 0) 
       sendEvent(name: 'Etc_season', value: 30)
       
       if((device.currentState('energy')?.doubleValue) <= 200)
       {
          def temp_charge = Math.round(basic_fare+month_energy*93.3)
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 

          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  ) 
          sendEvent(name: 'powerConsumption_step', value: "현재 누진1")       
       }
       else if((device.currentState('energy')?.doubleValue) <= 400)
       {
          def temp_charge = Math.round(basic_fare+(200*93.3)+((month_energy-200)*187.9))
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  )      
          sendEvent(name: 'powerConsumption_step', value: "현재 누진2")
       }
       else if((device.currentState('energy')?.doubleValue) <= 1000)
       {
          def temp_charge = Math.round(basic_fare+(200*93.3)+(200*187.9)+((month_energy-400)*280.6))
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  )      
          sendEvent(name: 'powerConsumption_step', value: "현재 누진3")        
       }
       else
       {
          def temp_charge = Math.round(basic_fare+(200*93.3)+(200*187.9)+(600*280.6)+((month_energy-1000)*709.5))
          def temp_tax1 = temp_charge*0.1
          def temp_tax2 = temp_charge*0.037 
          sendEvent(name: 'Electric_charges', value: Math.round(temp_charge+temp_tax1+temp_tax2)  )      
          sendEvent(name: 'powerConsumption_step', value: "현재 슈퍼누진4")          
       } 
    }
    
//log.debug Integer.parseInt(device.currentState('Electric_charges')?.doubleValue,1)

sendCharge(device.currentState('Electric_charges')?.doubleValue)
   
}

def updated() 
{
 	sendEvent(name: 'Meter_reading_date', value: Meter_reading_date) 
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() 
{
    return refresh()
}

def refresh() {
    log.debug "refresh "
    //createChild()
    zigbee.electricMeasurementPowerRefresh() +
           zigbee.simpleMeteringPowerRefresh()
}
def installed() 
{
    log.debug "Installed"
    sendEvent(name: "resetTotal", value: 0, unit: "kWh")
}
private void createChild() 
{
      log.debug "Creating child"
      def child = addChildDevice("smartthings","Zigbee Power Meter",
                                   "${device.deviceNetworkId}:${1}", 
                                                       device.hubId,
                                              [completedSetup: true, 
                                                  label: "전기세"])
}

private channelNumber(String dni) 
{
   dni.split(":")[-1] as Integer
}

private sendCharge(Double Charger ) 
{
   def descriptionText =  "실시간 전기요금"
   def child = childDevices?.find { channelNumber(it.deviceNetworkId) == 1 }
  
   if (child)
   {
      child?.sendEvent([name: "energy", value: Charger, data: [1: 1], descriptionText: descriptionText, isStateChange: true])
      child?.sendEvent([name: "power", value: Charger, data: [1: 1], descriptionText: descriptionText, isStateChange: true])     
   }
   else
   {
      log.debug "Child device not found!"
      createChild()      
   }  
}
def configure() 
{
    // this device will send instantaneous demand and current summation delivered every 1 minute
    sendEvent(name: "checkInterval", value: 2 * 60 + 10 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

    log.debug "Configuring Reporting"
    return refresh() +
           zigbee.simpleMeteringPowerConfig() +
           zigbee.electricMeasurementPowerConfig()
}