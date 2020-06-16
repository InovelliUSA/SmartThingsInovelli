/**
 *  Copyright 2019 Inovelli / Eric Maycock
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
 *  Inovelli Bulb Multi-White
 *
 *  Author: Eric Maycock
 *  Date: 2019-9-9
 * ported changes from hubitat drivers - bcopeland - 2020-02-26
 */

metadata {
	definition (name: "Inovelli Bulb Multi-White LZW41", namespace: "InovelliUSA", author: "erocm123",
				ocfDeviceType: "oic.d.light", mnmn: "SmartThings", vid: "generic-rgbw-color-bulb",
				runLocally: false, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false) {
		capability "Switch Level"
		capability "Color Temperature"
		capability "Switch"
		capability "Refresh"
		capability "Actuator"
		capability "Sensor"
		capability "Health Check"
        attribute "colorName", "string"
        attribute "firmware", "number"
        
        fingerprint mfr: "031E", prod: "0006", model: "0001", deviceJoinName: "Inovelli Bulb Multi-White" //US
        fingerprint deviceId: "0x1101", inClusters: "0x5E,0x85,0x59,0x86,0x72,0x5A,0x26,0x33,0x27,0x70,0x7A,0x73,0x98,0x7A"
        fingerprint deviceId: "0x1101", inClusters: "0x5E,0x98,0x86,0x85,0x59,0x72,0x73,0x26,0x33,0x70,0x27,0x5A,0x7A" // Secure
	}

	simulator {
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 1, height: 1, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState("on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff")
				attributeState("off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn")
				attributeState("turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff")
				attributeState("turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn")
			}

			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
	}
	preferences {
			input name: "colorStaging", type: "boolean", description: "", title: "Enable color pre-staging", defaultValue: false
			input name: "logEnable", type: "boolean", description: "", title: "Enable Debug Logging", defaultVaule: true
			input name: "bulbMemory", type: "enum", title: "Power outage state", options: ["0":"Remembers Last ON State","1":"Remembers Last State (ON or OFF)"], defaultValue: "0"
	}
	controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 2, inactiveLabel: false, range:"(2700..6500)") {
		state "colorTemperature", action:"color temperature.setColorTemperature"
	}
    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

	main(["switch"])
	details(["switch", "levelSliderControl", "colorTempSliderControl", "refresh"])
}

private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getWARM_WHITE_CONFIG() { 0x51 }
private getCOLD_WHITE_CONFIG() { 0x52 }
private getWARM_WHITE() { "warmWhite" }
private getCOLD_WHITE() { "coldWhite" }
private getWHITE_NAMES() { [WARM_WHITE, COLD_WHITE] }
private getZWAVE_COLOR_COMPONENT_ID() { [warmWhite: 0, coldWhite: 1, red: 2, green: 3, blue: 4] }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }


def updated() {
	log.debug "updated().."
    def cmds = []
    if (state.colorReceived==null || state.powerStateMem==null) initializeVars()
	if (state.powerStateMem?.toInteger() != bulbMemory?.toInteger()) {
        cmds = initializeConfig()
	    response(commands(cmds))
    }
}

def installed() {
	log.debug "installed()..."
	initializeVars()
	sendEvent(name: "checkInterval", value: 1860, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "0"])
	sendEvent(name: "level", value: 100, unit: "%")
	sendEvent(name: "colorTemperature", value: COLOR_TEMP_MIN)
}

def initializeVars() {
    log.debug "initializeVars()"
	if (state.colorReceived==null) state.colorReceived = [red: null, green: null, blue: null, warmWhite: null, coldWhite: null]
	if (state.powerStateMem==null) state.powerStateMem=0
}

def configure() {
    log.debug "configure()"
	def cmds = initializeConfig()
	return commands(cmds)
}

def initializeConfig() {
    def cmds = []
	cmds << zwave.configurationV1.configurationSet([scaledConfigurationValue: bulbMemory? bulbMemory.toInteger() : 0, parameterNumber: 2, size:1])
    cmds << zwave.configurationV1.configurationSet([scaledConfigurationValue: COLOR_TEMP_MIN, parameterNumber: WARM_WHITE_CONFIG, size: 2])
	cmds << zwave.configurationV1.configurationSet([scaledConfigurationValue: COLOR_TEMP_MAX, parameterNumber: COLD_WHITE_CONFIG, size: 2])
    cmds << zwave.configurationV1.configurationGet([parameterNumber: 2])
    state.cfgVersion=2
	return cmds
}

def parse(description) {
	def result = null
	if (description != "updated") {
        def cmd
        try {
		    cmd = zwave.parse(description,[0x33:2,0x26:2,0x86:1,0x70:1])
        } catch (e) {
            log.debug "An exception was caught $e"
        }
		if (cmd) {
			result = zwaveEvent(cmd)
			if (logEnable) log.debug("'$description' parsed to $result")
		} else {
			if (logEnable) log.debug("Couldn't zwave.parse '$description'")
		}
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    log.debug cmd
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv2.SwitchMultilevelReport cmd) {
    log.debug cmd
	unschedule(offlinePing)
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	if (logEnable) log.debug "got version report"
    // st doesn't support v2 this will need work
	def fw = cmd.applicationVersion + (cmd.applicationSubVersion / 100)
	state.firmware = fw
}

def zwaveEvent(physicalgraph.zwave.commands.switchcolorv2.SwitchColorReport cmd) {
	log.debug "got SwitchColorReport: $cmd"
	def result = []
    if (!state.colorReceived) initializeVars()
	state.colorReceived[cmd.colorComponent] = cmd.value
    if (WHITE_NAMES.every { state.colorReceived[it] != null}) {
		def warmWhite = state.colorReceived[WARM_WHITE]
		def coldWhite = state.colorReceived[COLD_WHITE]
		if (logEnable) log.debug "warmWhite: $warmWhite, coldWhite: $coldWhite"
		if (warmWhite == 0 && coldWhite == 0) {
			result = createEvent(name: "colorTemperature", value: COLOR_TEMP_MIN)
		} else {
			def colorTemp = COLOR_TEMP_MIN + (COLOR_TEMP_DIFF / 2)
			if (warmWhite != coldWhite) {
				colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF * warmWhite) / 255) as Integer
			}
			result << createEvent(name: "colorTemperature", value: colorTemp)
			setGenericTempName(colorTemp)
		}
		// Reset the values
		WHITE_NAMES.collect { state.colorReceived[it] = null }
	}
	result
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	def result = [createEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")]
	if (cmd.value) {
		result << createEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand()
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		createEvent(descriptionText: cmd.toString())
	}
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "got ConfigurationReport: $cmd"
	def result = null
    if (cmd.parameterNumber == 0x02) {
		state.powerStateMem = cmd.scaledConfigurationValue
    }
	result
}

def cmd2Integer(array) {
    switch(array.size()) {
        case 1:
            array[0]
            break
        case 2:
            ((array[0] & 0xFF) << 8) | (array[1] & 0xFF)
            break
        case 3:
            ((array[0] & 0xFF) << 16) | ((array[1] & 0xFF) << 8) | (array[2] & 0xFF)
            break
        case 4:
            ((array[0] & 0xFF) << 24) | ((array[1] & 0xFF) << 16) | ((array[2] & 0xFF) << 8) | (array[3] & 0xFF)
            break
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	def linkText = device.label ?: device.name
	[linkText: linkText, descriptionText: "$linkText: $cmd", displayed: false]
}

def buildOffOnEvent(cmd){
	[zwave.basicV1.basicSet(value: cmd), zwave.switchMultilevelV2.switchMultilevelGet()]
}

def on() {
    def cmds = []
	if (!state.cfgVersion || state.cfgVersion < 2) { 
    	cmds = initializeConfig() 
    }
    cmds << zwave.basicV1.basicSet(value: 0xFF)
	commands(cmds)
}

def off() {
	commands([zwave.basicV1.basicSet(value: 0x00)])
}

def refresh() {
	commands([zwave.switchMultilevelV2.switchMultilevelGet()] + queryAllColors())
}

def ping() {
	log.debug "ping().."
	unschedule(offlinePing)
	runEvery30Minutes(offlinePing)
	command(zwave.switchMultilevelV2.switchMultilevelGet())
}

def offlinePing() {
	log.debug "offlinePing()..."
	sendHubCommand(new physicalgraph.device.HubAction(command(zwave.switchMultilevelV2.switchMultilevelGet())))
}

def setLevel(level) {
	setLevel(level, 1)
}

def setLevel(level, duration) {
	log.debug "setLevel($level, $duration)"
	if(level > 99) level = 99
	commands([
		zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: duration)
	])
}

def setColorTemperature(temp) {
	log.debug "setColorTemperature($temp)"
	def cmds = []
	if (temp < COLOR_TEMP_MIN) temp = COLOR_TEMP_MIN
	if (temp > COLOR_TEMP_MAX) temp = COLOR_TEMP_MAX
	def warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF * 255) as Integer
	def coldValue = 255 - warmValue
	cmds << zwave.switchColorV2.switchColorSet(warmWhite: warmValue, coldWhite: coldValue)
    if ((device.currentValue("switch") != "on") && (!colorStaging)) {
        log.debug "Bulb is off. Turning on"
        cmds << zwave.basicV1.basicSet(value: 0xFF)
    }
	commands(cmds)
}

private queryAllColors() {
	WHITE_NAMES.collect { zwave.switchColorV2.switchColorGet(colorComponent: it) }
}

private secEncap(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private command(physicalgraph.zwave.Command cmd) {
    if (getZwaveInfo()?.zw?.contains("s")) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}

def setDefaultAssociations() {
    def smartThingsHubID = zwaveHubNodeId.toString().format( '%02x', zwaveHubNodeId )
    state.defaultG1 = [smartThingsHubID]
    state.defaultG2 = []
    state.defaultG3 = []
}

def setAssociationGroup(group, nodes, action, endpoint = null){
    if (!state."desiredAssociation${group}") {
        state."desiredAssociation${group}" = nodes
    } else {
        switch (action) {
            case 0:
                state."desiredAssociation${group}" = state."desiredAssociation${group}" - nodes
            break
            case 1:
                state."desiredAssociation${group}" = state."desiredAssociation${group}" + nodes
            break
        }
    }
}

def processAssociations(){
   def cmds = []
   setDefaultAssociations()
   def associationGroups = 5
   log.debug state.associationGroups
   if (state.associationGroups) {
       associationGroups = state.associationGroups
   } else {
       log.debug "Getting supported association groups from device"
       cmds <<  zwave.associationV2.associationGroupingsGet().format()
   }
   for (int i = 1; i <= associationGroups; i++){
      if(state."actualAssociation${i}" != null){
         if(state."desiredAssociation${i}" != null || state."defaultG${i}") {
            def refreshGroup = false
            ((state."desiredAssociation${i}"? state."desiredAssociation${i}" : [] + state."defaultG${i}") - state."actualAssociation${i}").each {
                log.debug "Adding node $it to group $i"
                cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:Integer.parseInt(it,16)).format()
                refreshGroup = true
            }
            ((state."actualAssociation${i}" - state."defaultG${i}") - state."desiredAssociation${i}").each {
                log.debug "Removing node $it from group $i"
                cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:Integer.parseInt(it,16)).format()
                refreshGroup = true
            }
            if (refreshGroup == true) cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
            else log.debug "There are no association actions to complete for group $i"
         }
      } else {
         log.debug "Association info not known for group $i. Requesting info from device."
         cmds << zwave.associationV2.associationGet(groupingIdentifier:i).format()
      }
   }
   return cmds
}

def setGenericTempName(temp){
    if (!temp) return
    def genericName
    def value = temp.toInteger()
    if (value <= 2000) genericName = "Sodium"
    else if (value <= 2100) genericName = "Starlight"
    else if (value < 2400) genericName = "Sunrise"
    else if (value < 2800) genericName = "Incandescent"
    else if (value < 3300) genericName = "Soft White"
    else if (value < 3500) genericName = "Warm White"
    else if (value < 4150) genericName = "Moonlight"
    else if (value <= 5000) genericName = "Horizon"
    else if (value < 5500) genericName = "Daylight"
    else if (value < 6000) genericName = "Electronic"
    else if (value <= 6500) genericName = "Skylight"
    else if (value < 20000) genericName = "Polar"
    def descriptionText = "${device.getDisplayName()} color is ${genericName}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}
