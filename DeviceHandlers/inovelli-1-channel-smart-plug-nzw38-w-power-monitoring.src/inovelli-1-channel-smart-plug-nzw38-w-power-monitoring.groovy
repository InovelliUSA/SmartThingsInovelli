/*
 *  Inovelli 1-Channel Smart Plug w/Energy Monitoring NZW38
 *  Author: Eric Maycock (erocm123)
 *  Date: 2018-05-29
 *
 *  Copyright 2018 Eric Maycock
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
 *  2018-05-02: Added support for Z-Wave Association Tool SmartApp. Associations require firmware 1.02+.
 *              https://github.com/erocm123/SmartThingsPublic/tree/master/smartapps/erocm123/z-waveat
 */
 
metadata {
    definition (name: "Inovelli 1-Channel Smart Plug NZW38 w/Energy Monitoring", namespace: "erocm123", author: "Eric Maycock") {
        capability "Switch"
        capability "Refresh"
        capability "Polling"
        capability "Actuator"
        capability "Sensor"
        capability "Health Check"
        capability "Button"
        capability "Configuration"
        
        attribute "lastActivity", "String"
        attribute "voltage", "number"
        
        command "setAssociationGroup", ["number", "enum", "number", "number"] // group number, nodes, action (0 - remove, 1 - add), multi-channel endpoint (optional)
        command "pressUpX2"

        fingerprint mfr: "0312", prod: "2600", model: "2600", deviceJoinName: "Inovelli Smart Plug w/Energy Monitoring"
        fingerprint deviceId: "0x1001", inClusters: "5E,0x25,0x32,0x70,0x71,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x5B,0x6C,0x7A"
    }

    simulator {
    }
    
    preferences {
        input "autoOff", "number", title: "Auto Off\n\nAutomatically turn switch off after this number of seconds\nRange: 0 to 32767", description: "Tap to set", required: false, range: "0..32767"
        input "ledIndicator", "enum", title: "LED Indicator\n\nTurn LED indicator on when switch is:\n", description: "Tap to set", required: false, options:[["0": "On"], ["1": "Off"], ["2": "Disable"]], defaultValue: "0"
        input "powerOnState", "enum", title: "Power On State\n\nReturn to this state after power failure:\n", description: "Tap to set", required: false, options:[["0": "Off"], ["1": "On"], ["2": "Previous"]], defaultValue: "2"
        input "reportTimeInt", "number", title: "Report Interval (Seconds)\n\nSend energy reports at this interval\nRange: 0 to 32767", description: "Tap to set", required: false, range: "0..32767"
        input "reportPowInt", "number", title: "Report Interval (Consumption)\n\nSend energy reports when power consumption changes by this much\nRange: 0 to 255", description: "Tap to set", required: false, range: "0..255"
        input "scene2Lower", "number", title: "Scene 2 Lower Threshold\n\nSend central scene report when power consumption reaches this lower threshold\nRange: 0 to 255", description: "Tap to set", required: false, range: "0..255"
        input "scene2Upper", "number", title: "Scene 2 Upper Threshold\n\nSend central scene report when power consumption reaches this upper threshold\nRange: 0 to 255", description: "Tap to set", required: false, range: "0..255"
        input "group3Lower", "number", title: "Group 3 Lower Threshold\n\nSend off command to group 3 when power consumption reaches this lower threshold\nRange: 0 to 255", description: "Tap to set", required: false, range: "0..255"
        input "group4Upper", "number", title: "Group 4 Lower Threshold\n\nSend off command to group 4 when power consumption reaches this upper threshold\nRange: 0 to 255", description: "Tap to set", required: false, range: "0..255"
        input description: "Use the \"Z-Wave Association Tool\" SmartApp to set device associations. (Firmware 1.02+)\n\nGroup 2: Sends on/off commands to associated devices when switch is pressed (BASIC_SET).", title: "Associations", displayDuringSetup: false, type: "paragraph", element: "paragraph"
    }
    
    tiles {
        multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
                attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
            }
        }
        valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
   state "default", label:'${currentValue} W'
  }
  valueTile("energy", "device.energy", decoration: "flat", width: 2, height: 2) {
   state "default", label:'${currentValue} kWh'
  }
  standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
   state "default", label:'reset kWh', action:"reset"
  }
        valueTile("voltage", "device.voltage", width: 2, height: 2) {
   state "default", label:'${currentValue} V'
  }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        
        valueTile("lastActivity", "device.lastActivity", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "default", label: 'Last Activity: ${currentValue}',icon: "st.Health & Wellness.health9"
        }
        valueTile("icon", "device.icon", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "default", label: '', icon: "https://inovelli.com/wp-content/uploads/Device-Handler/Inovelli-Device-Handler-Logo.png"
        }
    }
}

def installed() {
    refresh()
}

def configure() {
    log.debug "configure()"
    def cmds = initialize()
    commands(cmds)
}

def updated() {
    if (!state.lastRan || now() >= state.lastRan + 2000) {
        log.debug "updated()"
        state.lastRan = now()
        def cmds = initialize()
        response(commands(cmds))
    } else {
        log.debug "updated() ran within the last 2 seconds. Skipping execution."
    }
}

def initialize() {
    sendEvent(name: "checkInterval", value: 3 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    sendEvent(name: "numberOfButtons", value: 1, displayed: true)
    def cmds = processAssociations()
    log.debug powerOnState
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: powerOnState!=null? powerOnState.toInteger() : 2, parameterNumber: 2, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 2)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: ledIndicator!=null? ledIndicator.toInteger() : 1, parameterNumber: 3, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: autoOff!=null? autoOff.toInteger() : 0, parameterNumber: 5, size: 2)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 5)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: reportTimeInt!=null? reportTimeInt.toInteger() : 60, parameterNumber: 10, size: 2)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 10)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: reportPowInt!=null? reportPowInt.toInteger() : 10, parameterNumber: 11, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 11)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: scene2Lower!=null? scene2Lower.toInteger() : 0, parameterNumber: 12, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 12)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: scene2Upper!=null? scene2Upper.toInteger() : 0, parameterNumber: 13, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 13)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: group3Lower!=null? group3Lower.toInteger() : 0, parameterNumber: 14, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 14)
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: group4Upper!=null? groupUpper.toInteger() : 0, parameterNumber: 15, size: 1)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 15)

    return cmds
}

def parse(description) {
    def result = null
    if (description.startsWith("Err 106")) {
        state.sec = 0
        result = createEvent(descriptionText: description, isStateChange: true)
    } else if (description != "updated") {
        def cmd = zwave.parse(description, [0x20: 1, 0x32: 1, 0x25: 1, 0x70: 1, 0x98: 1])
        if (cmd) {
            result = zwaveEvent(cmd)
            log.debug("'$description' parsed to $result")
        } else {
            log.debug("Couldn't zwave.parse '$description'")
        }
    }
    def now
    if(location.timeZone)
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    else
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a")
    sendEvent(name: "lastActivity", value: now, displayed:false)
    result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x25: 1])
    if (encapsulatedCommand) {
        state.sec = 1
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
    def event
 if (cmd.scale == 0) {
     if (cmd.meterType == 161) {
      event = createEvent(name: "voltage", value: cmd.scaledMeterValue, unit: "V")
        } else if (cmd.meterType == 33) {
         event = createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
        }
 } else if (cmd.scale == 1) {
  event = createEvent(name: "amperage", value: cmd.scaledMeterValue, unit: "A")
 } else if (cmd.scale == 2) {
  event = createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W")
 }

    return event
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
    createEvent(buttonEvent(cmd.sceneNumber, (cmd.sceneNumber == 2? "held" : "pushed"), "physical"))
}

def buttonEvent(button, value, type = "digital") {
    sendEvent(name:"lastEvent", value: "${value != 'pushed'?' Tap '.padRight(button+1+5, '▼'):' Tap '.padRight(button+1+5, '▲')}", displayed:false)
    [name: "button", value: value, data: [buttonNumber: button], descriptionText: "$device.displayName button $button was $value", isStateChange: true, type: type]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "Unhandled: $cmd"
    null
}

def on() {
    commands([
        zwave.basicV1.basicSet(value: 0xFF),
        zwave.switchBinaryV1.switchBinaryGet()
    ])
}

def off() {
    commands([
        zwave.basicV1.basicSet(value: 0x00),
        zwave.switchBinaryV1.switchBinaryGet()
    ])
}

def ping() {
    refresh()
}

def poll() {
    refresh()
}

def refresh() {
    commands([
              zwave.switchBinaryV1.switchBinaryGet(),
              zwave.meterV2.meterGet(scale: 0),
              zwave.meterV2.meterGet(scale: 1),
        zwave.meterV2.meterGet(scale: 2)
             ])
}

private command(physicalgraph.zwave.Command cmd) {
    if (state.sec) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay=500) {
    delayBetween(commands.collect{ command(it) }, delay)
}

def pressUpX2() {
    sendEvent(buttonEvent(1, "pushed"))
}

def setDefaultAssociations() {
    def smartThingsHubID = zwaveHubNodeId.toString().format( '%02x', zwaveHubNodeId )
    state.defaultG1 = [smartThingsHubID]
    state.defaultG2 = []
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
   if (state.associationGroups) {
       associationGroups = state.associationGroups
   } else {
       log.debug "Getting supported association groups from device"
       cmds <<  zwave.associationV2.associationGroupingsGet()
   }
   for (int i = 1; i <= associationGroups; i++){
      if(state."actualAssociation${i}" != null){
         if(state."desiredAssociation${i}" != null || state."defaultG${i}") {
            def refreshGroup = false
            ((state."desiredAssociation${i}"? state."desiredAssociation${i}" : [] + state."defaultG${i}") - state."actualAssociation${i}").each {
                log.debug "Adding node $it to group $i"
                cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))
                refreshGroup = true
            }
            ((state."actualAssociation${i}" - state."defaultG${i}") - state."desiredAssociation${i}").each {
                log.debug "Removing node $it from group $i"
                cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))
                refreshGroup = true
            }
            if (refreshGroup == true) cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
            else log.debug "There are no association actions to complete for group $i"
         }
      } else {
         log.debug "Association info not known for group $i. Requesting info from device."
         cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
      }
   }
   return cmds
}

void zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    def temp = []
    if (cmd.nodeId != []) {
       cmd.nodeId.each {
          temp += it.toString().format( '%02x', it.toInteger() ).toUpperCase()
       }
    } 
    state."actualAssociation${cmd.groupingIdentifier}" = temp
    log.debug "Associations for Group ${cmd.groupingIdentifier}: ${temp}"
    updateDataValue("associationGroup${cmd.groupingIdentifier}", "$temp")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    log.debug "Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
    createEvent(name: "groups", value: cmd.supportedGroupings)
}

void zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
    log.debug cmd
    if(cmd.applicationVersion && cmd.applicationSubVersion) {
     def firmware = "${cmd.applicationVersion}.${cmd.applicationSubVersion.toString().padLeft(2,'0')}"
        state.needfwUpdate = "false"
        sendEvent(name: "status", value: "fw: ${firmware}")
        updateDataValue("firmware", firmware)
    }
}
