/**
 *  RGB Child Device
 *  Author: Eric Maycock (erocm123)
 *  Date: 2020-07-09
 *
 *  Copyright 2020 Inovelli / Eric Maycock
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


metadata {
	definition (name: "RGB Child Device", namespace: "InovelliUSA", author: "Eric Maycock", ocfDeviceType: "oic.d.light", mnmn: "SmartThings", vid: "generic-rgbw-color-bulb") {
	capability "Switch"		
	capability "Switch Level"
	capability "Actuator"
	capability "Color Control"
	capability "Sensor"
	capability "Light"
  }

	simulator {
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.illuminance.illuminance.bright", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.illuminance.illuminance.dark", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.illuminance.illuminance.light", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.illuminance.illuminance.light", backgroundColor:"#ffffff", nextState:"turningOn"
			}    
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
        			attributeState "level", action:"switch level.setLevel"
    			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"color control.setColor"
			}
		}

		main(["switch"])
		details(["switch", "level", "color"])
	}
}

def parse(Map description) {
    def eventMap
    if (description.type == null) eventMap = [name:"$description.name", value:"$description.value"]
    else eventMap = [name:"$description.name", value:"$description.value", type:"$description.type"]
    createEvent(eventMap)
}

def parse(description){
    log.debug description
    //if (description.name && description.value)sendEvent(name: description.name, value: description.value)
}

def on() {
    parent.childOn(device.deviceNetworkId)
}

def off() {
    parent.childOff(device.deviceNetworkId)
}

def refresh() {
	parent.childRefresh(device.deviceNetworkId)
}

def setLevel(value) {
	parent.childSetLevel(device.deviceNetworkId, value)
}

def setLevel(value, duration) {
	setLevel(value)
}

def setColor(Map color) {
    parent.componentSetColor(device.deviceNetworkId, color)
}

def installed() {
}
