/**
 *  Copyright 2020 Eric Maycock
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
	definition (name: "Switch Level Child Device", namespace: "InovelliUSA", author: "Eric Maycock", vid: "generic-dimmer") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Refresh"
        
                command "onPhysical"
		command "offPhysical"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "level", label:'${currentValue} %', unit:"%", backgroundColor:"#ffffff"
		}
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		

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
    sendEvent(name:"switch", value:"on")
	if (!parent.installedSmartApp) parent.childOn(device.deviceNetworkId)
}

def off() {
    sendEvent(name:"switch", value:"off")
	if (!parent.installedSmartApp) parent.childOff(device.deviceNetworkId)
}

def onPhysical() {
	log.debug "$version onPhysical()"
	sendEvent(name: "switch", value: "on", type: "physical")
}

def offPhysical() {
	log.debug "$version offPhysical()"
	sendEvent(name: "switch", value: "off", type: "physical")
}

def refresh() {
	if (!parent.installedSmartApp) parent.childRefresh(device.deviceNetworkId)
}

def setLevel(value) {
	log.debug "setLevel >> value: $value"
	def level = Math.max(Math.min(value as Integer, 99), 0)
	if (level > 0) {
		sendEvent(name: "switch", value: "on")
	} else {
		sendEvent(name: "switch", value: "off")
	}
	sendEvent(name: "level", value: level, unit: "%")
    if (!parent.installedSmartApp) parent.childSetLevel(device.deviceNetworkId, value)
}

def setLevel(value, duration) {
	log.debug "setLevel >> value: $value, duration: $duration"
	setLevel(value)
}
