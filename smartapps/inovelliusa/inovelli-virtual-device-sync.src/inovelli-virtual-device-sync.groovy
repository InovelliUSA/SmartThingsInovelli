/**
 *  Inovelli Virtual Device Sync (VDS)
 *
 *  Copyright 2020 Eric Maycock / Inovelli
 * 
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
 
definition(
   name: "Inovelli Virtual Device Sync",
   namespace: "InovelliUSA",
   author: "Eric Maycock",
   description: "Creates virtual devices and keeps them in sync with the selected physical device. Intended to be used with devices with multiple channels: LZW36 Fan + Light, 2-Channel Plug, etc.",
   category: "Convenience",
   iconUrl: "https://raw.githubusercontent.com/InovelliUSA/SmartThingsInovelli/master/smartapps/inovelliusa/inovelli-virtual-device-sync.src/inovelli-virtual-device-sync.png",
   iconX2Url: "https://raw.githubusercontent.com/InovelliUSA/SmartThingsInovelli/master/smartapps/inovelliusa/inovelli-virtual-device-sync.src/inovelli-virtual-device-sync-2x.png",
   iconX3Url: "https://raw.githubusercontent.com/InovelliUSA/SmartThingsInovelli/master/smartapps/inovelliusa/inovelli-virtual-device-sync.src/inovelli-virtual-device-sync-3x.png"
)

preferences {
    page(name: "setupPage")
    page(name: "createVirtual")
    page(name: "removeVirtual")
    page(name: "removalPage")
    page(name: "createPage")
}

def setupPage() {
    dynamicPage(name: "setupPage", install: true, uninstall: true) {
    section { 
        if(!isVirtualConfigured()){
           input "physical", "capability.switch", title: "Which Physical Device", multiple: false, required: true, submitOnChange: true
           if(physical){
              paragraph "Device Handler: $physical.typeName\r\n\r\nDetected Number of Endpoints: ${getEndpoints()}\r\n\r\nRecommended Type: ${getType()}"
              input "virtualSwitchType", "enum", title: "Virtual Switch Type", value: getType() , multiple: false, required: true, options: ["Switch","Energy Switch","Dimmer"]
              app.updateSetting("virtualSwitchType", getType())
           } 
           href "createVirtual", title:"Create Virtual Devices", description:"Create virtual devices"
        } else {
           def switchNames = ""
           getChildDevices().each {
               switchNames = switchNames + it.displayName + "\r\n"
           }
           paragraph "Chosen Device: ${physical?physical:'None'}\r\n\r\nTo change to a different device, please remove the virtual devices below."
           if (!physical) {
               paragraph "Physical device has been removed but virtual devices remain. Please remove virtual devices below."
           } else {
               paragraph "Device Handler: $physical.typeName\r\n\r\nDetected Number of Endpoints: ${getEndpoints()}\r\n\r\nRecommended Type: ${getType()}\r\n\r\nVirtual Switches have been created. They will be kept in sync with the physical switch chosen above\r\n\r\n$switchNames"
           }
           href "removeVirtual", title:"Remove Virtual Devices", description:"Remove virtual devices"
        }
    }
    section([title:"Available Options", mobileOnly:true]) {
            input "setLabel", "bool", title: "Change the default name of the app?", required: false, submitOnChange: true, value: false
            if (settings.setLabel != null && setLabel == true) {
               label title:"Assign a name for your app (optional)", required:false
            } 
		}
    }
}

def createVirtual(){
   dynamicPage(name: "createVirtual", title: "Associate your device's endpoints with virtual devices", nextPage: "createPage") {
		section {
			paragraph "This process will create virtual devices and associate them with the endpoints on your physical device."
            def switchNames = ""
            for (int i = 1; i <= getEndpoints(); i++){
              if (physical.typeName.toUpperCase().indexOf("INOVELLI FAN + LIGHT LZW36") >= 0) {
                   switchNames = switchNames + "$physical.displayName - ${i==1?'Light':'Fan'}\r\n"
               } else {
                   switchNames = switchNames + "$physical.displayName - ${i}\r\n"
               }
            }
            paragraph "The following switches will be created:\r\n\r\n" + switchNames
		}
    }
}

def createPage(){
   dynamicPage(name: "createPage", title: "Devices have been created", nextPage: "setupPage", createVirtualDevice())
}

def removeVirtual(){
   def switchNames = ""
   dynamicPage(name: "removeVirtual", title: "Remove the virtual switches created by this app", nextPage: "removalPage") {
		section {
			paragraph "This process will remove the virtual switches created by this program. Press next to continue"
            getChildDevices().each {
               switchNames = switchNames + it.displayName + "\r\n"
            }
            paragraph "The following virtual switches will be removed:\r\n\r\n" + switchNames
		}
    }
}

def removalPage(){
   dynamicPage(name: "removalPage", title: "Devices have been removed", nextPage: "setupPage", removeVirtualDevice()) 
}

def createVirtualDevice() {
    if(!isVirtualConfigured()){
       def switchName
       try {
          for (int i = 1; i <= getEndpoints(); i++){
             if (physical.typeName.toUpperCase().indexOf("INOVELLI FAN + LIGHT LZW36") >= 0) {
                 switchName = "$physical.displayName - ${i==1?'Light':'Fan'}"
             } else {
                 switchName = "$physical.displayName - ${i}"
             }
             def switchType = ""
             if (virtualSwitchType != null && virtualSwitchType == "Switch"){
                switchType = "Simulated Switch"
             } else if (virtualSwitchType != null && virtualSwitchType == "Energy Switch") {
                switchType = "Simulated Energy Switch"
             } else {
                switchType = "Switch Level Child Device"
             }
             def child = addChildDevice("InovelliUSA", switchType, getDeviceID(i), null, [name: getDeviceID(i), label: switchName, completedSetup: true])
          }   
       } catch (e) {
          return {
		   section {
			   paragraph "Error when creating the virtual devices. Make sure that you have all of the \"Simulated\" device handlers installed."
		   }
       }
    }
    return {
	   section {
	      paragraph "Devices have been configured. Press next to go to the main page."
	   }
    }
    }else{
       return {
	      section {
		     paragraph "Devices have already been configured."
		  }
       }
    }
}

def isVirtualConfigured(){ 
    def foundDevice = false
    getChildDevices().each {
       foundDevice = true
    }
    return foundDevice
}

def removeVirtualDevice() {
    try {
       unsubscribe()
       getChildDevices().each {
          deleteChildDevice(it.deviceNetworkId)
       }
       return {
          section {
	         paragraph "Devices have been removed. Press next to go to the main page."
	      }
       }
	} catch (e) {
       return {
          section {
			paragraph "Error: ${(e as String).split(":")[1]}."
	      }
       }
    }
}

private getDeviceID(number) {
    return "${app.id}/${number}"
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  if(physical != null && setLabel != null && setLabel != true){
     app.updateLabel("Inovelli VDS - ${physical.label ? physical.label : physical.name}")
  }
  unsubscribe()
  initialize()
}

def initialize() {
  log.debug "Initializing Virtual Device Sync"
  for (int i = 1; i <= getEndpoints(); i++){
        subscribe(physical, "switch${i}", physicalHandler)
        subscribe(physical, "power${i}", powerHandler)
        subscribe(physical, "energy${i}", energyHandler)
        subscribe(physical, "level${i}", physicalHandler)
  }
  getChildDevices().each {
     subscribeToCommand(it, "on", virtualHandler)
     subscribeToCommand(it, "off", virtualHandler)
     subscribeToCommand(it, "setLevel", virtualHandler)
  }
}

def virtualHandler(evt) {
  log.debug "virtualHandler called with event: deviceId ${evt.deviceId} name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}"
    getChildDevices().each {
       if (evt.deviceId == it.id) {
          def switchNumber = it.deviceNetworkId.split("/")[1]
             switch (evt.value){
                case "setLevel":
                   physical."setLevel${switchNumber}"(it.currentValue("level"))
                break
                default:
                   physical."${evt.value}${switchNumber}"()
                break
             }
          }
       }
}

def physicalHandler(evt) {
  log.debug "physicalHandler called with event:  name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}"
     switch(evt.name){
        case ~/.*level.*/:
           sendEvent(getChildDevice("${app.id}/${getSwitchNumber(evt.name)}"), [name:"level", value:"$evt.value", type:"physical"])
        break
        default:
           sendEvent(getChildDevice("${app.id}/${getSwitchNumber(evt.name)}"), [name:"switch", value:"$evt.value", type:"physical"])
        break
     }
}

def powerHandler(evt) {
   log.debug "powerHandler called with event:  name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}"
   sendEvent(getChildDevice("${app.id}/${getSwitchNumber(evt.name)}"), [name:"power", value:"$evt.value"])
}

def energyHandler(evt) {
   log.debug "energyHandler called with event:  name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}"
   sendEvent(getChildDevice("${app.id}/${getSwitchNumber(evt.name)}"), [name:"energy", value:"$evt.value"])
}

private getSwitchNumber(value){
   switch (value) {
      case ~/.*switch.*/:
         return value.substring(6).toInteger()
      break
      case ~/.*energy.*/:
         return value.substring(6).toInteger()
      break
      case ~/.*power.*/:
         return value.substring(5).toInteger()
      break
      case ~/.*level.*/:
         return value.substring(5).toInteger()
      break
   }
}

private getEndpoints() {
   def endpoints = 0
      physical.supportedCommands.each {
         switch (it) {     
            case ~/.*on.*/:
               for (int i = 1; i <= 10; i++){
                  if (it.toString().indexOf("$i") >= 0) if (i > endpoints) endpoints = i
               }
            break
           }
       }
   return endpoints
}

private getType() {
   String hasCapability = ""
   
   if (physical.hasCapability("Switch")) {
      hasCapability = "Switch"
   }
   if ((physical.hasCapability("Power Meter")) || (physical.hasCapability("Energy Meter"))) {
      hasCapability = "Energy Switch"
   }
   if (physical.hasCapability("Switch Level")) {
      hasCapability = "Dimmer"
   }
   return hasCapability
}
