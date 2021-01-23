/**
 *  Inovelli Light Strip LZW45
 *  Author: Eric Maycock (erocm123)
 *  Date: 2021-01-23
 *  Platform: SmartThings
 *
 *  ******************************************************************************************************
 *
 *  IMPORTANT INFORMATION:
 *      Quick Effect Calculator. You can create Quick Effect child devices is the preferences 
 *      section to get the desired effect. 
 *
 *      You can use the custom effect calculator located below. Thank you Nathan Fiscus for making it!:
 *      https://nathanfiscus.github.io/inovelli-led-strip-toolbox/
 *
 *  ******************************************************************************************************
 *
 *  Copyright 2020 Eric Maycock / Inovelli
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
 *  2021-01-23: Adding color prestaging option. 
 *
 */

import groovy.transform.Field

metadata {
    definition (name: "Inovelli Light Strip LZW45", namespace: "InovelliUSA", author: "InovelliUSA", ocfDeviceType: "oic.d.light", vid: "generic-rgbw-color-bulb") {
        capability "ColorTemperature"
        capability "ColorControl"
        capability "Switch"
        capability "Refresh"
        capability "Actuator"
        capability "Sensor"
        capability "Configuration"
        capability "ColorMode"
        capability "Button"
        capability "Switch Level"
        capability "Energy Meter"
        capability "Power Meter"

        attribute "colorName", "string"
        attribute "lastActivity", "String"
        attribute "lastEvent", "String"
        attribute "firmware", "String"
        attribute "groups", "Number"
        
        command "childOn", ["string"]
        command "childOff", ["string"]
        command "childSetLevel", ["string"]
        command "childRefresh", ["string"]
        command "componentOn"
        command "componentOff"
        command "componentSetLevel"
        command "componentRefresh"
        
        command "pixelEffectStart", ["number", "number"]
        command "pixelEffectStop"
        command "pixelEffectNext"
        command "pixelEffectPrevious"
        
        command "customEffectStart",  ["string"]
        command "customEffectStop"
        
        fingerprint manufacturer: "031E", prod: "000A", model: "0001", deviceJoinName: "Inovelli Light Strip"
        fingerprint deviceId: "0x1101", inClusters:"0x5E,0x86,0x33,0x72,0x5A,0x85,0x59,0x73,0x32,0x26,0x70,0x75,0x22,0x8E,0x55,0x98,0x9F,0x6C,0x7A,0x5B,0x87"
    }
    preferences {
        getParameterNumbers().each{ i ->
            switch(configParams["parameter${i.toString().padLeft(3,"0")}"].type)
            {   
                case "number":
                    input "parameter${i}", "number",
                        title:configParams["parameter${i.toString().padLeft(3,"0")}"].name + "\n" + configParams["parameter${i.toString().padLeft(3,"0")}"].description + "\nRange: " + configParams["parameter${i.toString().padLeft(3,"0")}"].range + "\nDefault: " + configParams["parameter${i.toString().padLeft(3,"0")}"].default,
                        range: configParams["parameter${i.toString().padLeft(3,"0")}"].range
                break
                case "enum":
                    input "parameter${i}", "enum",
                        title: configParams["parameter${i.toString().padLeft(3,"0")}"].name, // + getParameterInfo(i, "description"),
                        options: configParams["parameter${i.toString().padLeft(3,"0")}"].range
                break
                if (i == 9){
                    input "parameter9level", "number", 
                    title: "Default Level", 
                    description: "\nThis option is used in conjuction with the Default Color. This allows you to set the default color and level that will be used when the device is turned on.", 
                    required: false,
                    range: "0..360"
                }
            }
        }

        [201,202,203,204,205].each { i ->
            input "parameter21-${i}a", "enum", title: "Quick Effect ${i-200} - Color", description: "Tap to set", 
                displayDuringSetup: false, required: false, options: [
                0:"Red", 21:"Orange", 42:"Yellow", 85:"Green", 127:"Cyan", 170:"Blue", 212:"Violet", 234:"Pink", 2700:"Warm White", 4500:"White", 6500:"Cold White", 361:"Random"]
            input "parameter21-${i}b", "enum", title: "Quick Effect ${i-200} - Level", description: "Tap to set", 
                displayDuringSetup: false, required: false, options: [
                0:"0%", 1:"10%", 2:"20%", 3:"30%", 4:"40%", 5:"50%", 6:"60%", 7:"70%", 8:"80%", 9:"90%", 10:"100%"]
            input "parameter21-${i}c", "enum", title: "Quick Effect ${i-200} - Duration", description: "Tap to set", 
                displayDuringSetup: false, required: false, options: [
                    255:"Indefinitely", 1:"1 Second", 2:"2 Seconds", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds", 8:"8 Seconds", 
                    9:"9 Seconds", 10:"10 Seconds", 11:"11 Seconds", 12:"12 Seconds", 13:"13 Seconds", 14:"14 Seconds", 15:"15 Seconds", 16:"16 Seconds", 
                    17:"17 Seconds", 18:"18 Seconds", 19:"19 Seconds", 20:"20 Seconds", 21:"21 Seconds", 22:"22 Seconds", 23:"23 Seconds", 24:"24 Seconds", 
                    25:"25 Seconds", 26:"26 Seconds", 27:"27 Seconds", 28:"28 Seconds", 29:"29 Seconds", 30:"30 Seconds", 31:"31 Seconds", 32:"32 Seconds", 
                    33:"33 Seconds", 34:"34 Seconds", 35:"35 Seconds", 36:"36 Seconds", 37:"37 Seconds", 38:"38 Seconds", 39:"39 Seconds", 40:"40 Seconds", 
                    41:"41 Seconds", 42:"42 Seconds", 43:"43 Seconds", 44:"44 Seconds", 45:"45 Seconds", 46:"46 Seconds", 47:"47 Seconds", 48:"48 Seconds", 
                    49:"49 Seconds", 50:"50 Seconds", 51:"51 Seconds", 52:"52 Seconds", 53:"53 Seconds", 54:"54 Seconds", 55:"55 Seconds", 56:"56 Seconds", 
                    57:"57 Seconds", 58:"58 Seconds", 59:"59 Seconds", 61:"1 Minute", 62:"2 Minutes", 63:"3 Minutes", 64:"4 Minutes", 65:"5 Minutes", 66:"6 Minutes", 
                    67:"7 Minutes", 68:"8 Minutes", 69:"9 Minutes", 70:"10 Minutes", 71:"11 Minutes", 72:"12 Minutes", 73:"13 Minutes", 74:"14 Minutes", 
                    75:"15 Minutes", 76:"16 Minutes", 77:"17 Minutes", 78:"18 Minutes", 79:"19 Minutes", 80:"20 Minutes", 81:"21 Minutes", 82:"22 Minutes", 
                    83:"23 Minutes", 84:"24 Minutes", 85:"25 Minutes", 86:"26 Minutes", 87:"27 Minutes", 88:"28 Minutes", 89:"29 Minutes", 90:"30 Minutes", 
                    91:"31 Minutes", 92:"32 Minutes", 93:"33 Minutes", 94:"34 Minutes", 95:"35 Minutes", 96:"36 Minutes", 97:"37 Minutes", 98:"38 Minutes", 
                    99:"39 Minutes", 100:"40 Minutes", 101:"41 Minutes", 102:"42 Minutes", 103:"43 Minutes", 104:"44 Minutes", 105:"45 Minutes", 106:"46 Minutes", 
                    107:"47 Minutes", 108:"48 Minutes", 109:"49 Minutes", 110:"50 Minutes", 111:"51 Minutes", 112:"52 Minutes", 113:"53 Minutes", 114:"54 Minutes", 
                    115:"55 Minutes", 116:"56 Minutes", 117:"57 Minutes", 118:"58 Minutes", 119:"59 Minutes", 121:"1 Hour", 122:"2 Hours", 123:"3 Hours", 
                    124:"4 Hours", 125:"5 Hours", 126:"6 Hours", 127:"7 Hours", 128:"8 Hours", 129:"9 Hours", 130:"10 Hours", 131:"11 Hours", 132:"12 Hours", 
                    133:"13 Hours", 134:"14 Hours", 135:"15 Hours", 136:"16 Hours", 137:"17 Hours", 138:"18 Hours", 139:"19 Hours", 140:"20 Hours", 141:"21 Hours", 
                    142:"22 Hours", 143:"23 Hours", 144:"24 Hours", 145:"25 Hours", 146:"26 Hours", 147:"27 Hours", 148:"28 Hours", 149:"29 Hours", 150:"30 Hours", 
                    151:"31 Hours", 152:"32 Hours", 153:"33 Hours", 154:"34 Hours", 155:"35 Hours", 156:"36 Hours", 157:"37 Hours", 158:"38 Hours", 159:"39 Hours", 
                    160:"40 Hours", 161:"41 Hours", 162:"42 Hours", 163:"43 Hours", 164:"44 Hours", 165:"45 Hours", 166:"46 Hours", 167:"47 Hours", 168:"48 Hours", 
                    169:"49 Hours", 170:"50 Hours", 171:"51 Hours", 172:"52 Hours", 173:"53 Hours", 174:"54 Hours", 175:"55 Hours", 176:"56 Hours", 177:"57 Hours", 
                    178:"58 Hours", 179:"59 Hours", 180:"60 Hours", 181:"61 Hours", 182:"62 Hours", 183:"63 Hours", 184:"64 Hours", 185:"65 Hours", 186:"66 Hours", 
                    187:"67 Hours", 188:"68 Hours", 189:"69 Hours", 190:"70 Hours", 191:"71 Hours", 192:"72 Hours", 193:"73 Hours", 194:"74 Hours", 195:"75 Hours", 
                    196:"76 Hours", 197:"77 Hours", 198:"78 Hours", 199:"79 Hours", 200:"80 Hours", 201:"81 Hours", 202:"82 Hours", 203:"83 Hours", 204:"84 Hours", 
                    205:"85 Hours", 206:"86 Hours", 207:"87 Hours", 208:"88 Hours", 209:"89 Hours", 210:"90 Hours", 211:"91 Hours", 212:"92 Hours", 213:"93 Hours", 
                    214:"94 Hours", 215:"95 Hours", 216:"96 Hours", 217:"97 Hours", 218:"98 Hours", 219:"99 Hours", 220:"100 Hours", 221:"101 Hours", 222:"102 Hours", 
                    223:"103 Hours", 224:"104 Hours", 225:"105 Hours", 226:"106 Hours", 227:"107 Hours", 228:"108 Hours", 229:"109 Hours", 230:"110 Hours", 
                    231:"111 Hours", 232:"112 Hours", 233:"113 Hours", 234:"114 Hours", 235:"115 Hours", 236:"116 Hours", 237:"117 Hours", 238:"118 Hours", 
                    239:"119 Hours", 240:"120 Hours", 241:"121 Hours", 242:"122 Hours", 243:"123 Hours", 244:"124 Hours", 245:"125 Hours", 246:"126 Hours", 
                    247:"127 Hours", 248:"128 Hours", 249:"129 Hours", 250:"130 Hours", 251:"131 Hours", 252:"132 Hours", 253:"133 Hours", 254:"134 Hours"]
            input "parameter21-${i}d", "enum", title: "Quick Effect ${i-200} - Effect", description: "Tap to set", displayDuringSetup: false, required: false, options: [
                0:"Off", 1:"Solid", 2:"Chase", 3:"Fast Blink", 4:"Slow Blink", 5:"Fast Fade", 6:"Slow Fade"]
        }
        
        [211,212,213,214,215].each { i ->
            input name: "pixelEffectString${i}", type: "string", 
                title: "Pixel Effect String ${i-210}", description: "Input a pixel effect number (1-46) and a brightness level separated by a , (i.e. 45,75). This will create a pixel effect child device that can be used to turn the effect on/off"
        }
        
        [221,222,223,224,225].each { i ->
            input name: "customEffectString${i}", type: "string", 
                title: "Custom Effect String ${i-220}", description: "Input a custom effect string (i.e. 121,99,3,66047). This will create a pixel effect child device that can be used to turn the effect on/off"
        }
        input name: "colorStaging", type: "bool", description: "", title: "Enable color pre-staging", defaultValue: false
        input description: "Use the below options to enable child devices for the specified settings. This will allow you to adjust these settings using " +
            "SmartApps such as Smart Lighting.", title: "Child Devices", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input "enableDefaultLocalChild", "bool", title: "Create \"Default Level (Local)\" Child Device", description: "", required: false, defaultValue: "false"
        input "enableDefaultZWaveChild", "bool", title: "Create \"Default Level (Z-Wave)\" Child Device", description: "", required: false, defaultValue: "false"

        input description: "Use the options below to enable or disable logs in case they are needed to provide addtional information on how the device is functioning.",
            title: "Log Settings", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input name: "debugEnable", type: "bool", 
            title: "Enable Debug Logging", defaultValue: true
        input name: "infoEnable", type: "bool", 
            title: "Enable Informational Logging", defaultValue: true
        input name: "disableDebugLogging", type: "number", 
            title: "Disable Debug Logging: Disable debug logging after this number of minutes (0=Do not disable)", defaultValue: 0
        input name: "disableInfoLogging", type: "number", 
            title: "Disable Info Logging: Disable info logging after this number of minutes (0=Do not disable)", defaultValue: 30
    }
}

@Field static List ledNotificationEndpoints = [21]
@Field static int maxPixelEffects = 46
@Field static int COLOR_TEMP_MIN=2700
@Field static int COLOR_TEMP_MAX=6500
@Field static List<String> RGB_NAMES=["red", "green", "blue"]
@Field static List<String> WHITE_NAMES=["warmWhite", "coldWhite"]
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }

@Field static Map configParams = [
    parameter001 : [
        number: 1,
        name: "# Of Pixels",
        description: "When individually addressable LEDs are used, this parameter tells the controller the number of pixels that are attached. \n·0 - Automatic recognition of pixels\n1..130 - Set the fixed value of the pixel bit",
        range: "0..130",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter002 : [
        number: 2,
        name: "Dimming Speed",
        description: "This changes the speed in which the light strip dims up or down. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed.\n\n0 - Instant\n1 - Fast\n..\n..\n98 - Slow",
        range: "0..98",
        default: 3,
        size: 1,
        type: "number",
        value: null
        ],
    parameter003 : [
        number: 3,
        name: "Ramp Rate",
        description: "This changes the speed in which the light strip turns on or off. For example, when a user sends the switch a basicSet(value: 0xFF) or basicSet(value: 0x00), this is the speed in which those actions take place. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed. A setting of 99 should keep this in sync with parameter 2.\n\n0 - Instant\n1 - Fast\n..\n98 - Slow\n99 - Keep in sync with parameter 2",
        range: "0..99",
        default: 99,
        size: 1,
        type: "number",
        value: null
        ],
    parameter004 : [
        number: 4,
        name: "Minimum Level",
        description: "The minimum level that the strip can be dimmed to. Useful when the user has an LED strip that does not turn on or flickers at a lower level.",
        range: "1..45",
        default: 1,
        size: 1,
        type: "number",
        value: null
        ],
    parameter005 : [
        number: 5,
        name: "Maximum Level",
        description: "The maximum level that the strip can be dimmed to. Useful when the user has an LED strip that reaches its maximum level before the dimmer value of 99.",
        range: "55..99",
        default: 99,
        size: 1,
        type: "number",
        value: null
        ],
    parameter006 : [
        number: 6,
        name: "Auto Off Timer ",
        description: "Automatically turns the strip off after this many seconds. When the strip is turned on a timer is started that is the duration of this setting. When the timer expires, the strip is turned off.\n0 - Auto off is disabled",
        range: "0..32767",
        default: 0,
        size: 2,
        type: "number",
        value: null
        ],
    parameter007 : [
        number: 7,
        name: "Default Level (Local)",
        description: "Default level for the strip when it is powered on from the local switch. A setting of 0 means that the switch will return to the level that it was on before it was turned off.\n0 - Previous",
        range: "0..99",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter008 : [
        number: 8,
        name: "Default Level (Z-Wave)",
        description: "Default level for the dimmer when it is powered on from a Z-Wave command (i.e. BasicSet(0xFF). A setting of 0 means that the switch will return to the level that it was on before it was turned off.\n0 - Previous",
        range: "0..99",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter009 : [
        number: 9,
        name: "Default Color",
        description: "Byte(3-2): Values between 2700-6500 represent a color temperature. Byte(1-0): Values between 0-361, represent the color on the Hue color wheel. The value of 361 represents a random color.\n",
        range: "0..6500",
        default: 2700,
        size: 4,
        type: "number",
        value: null
        ],
    parameter010 : [
        number: 10,
        name: "State after power Restored",
        description: "The state the switch should return to once power is restored after power failure.\n0 - Off\n1 - Default Color / Level (Parameter 9)\n2 - Previous",
        range: "0..2",
        default: 2,
        size: 1,
        type: "number",
        value: null
        ],
    parameter021 : [
        number: 21,
        name: "Quick Strip Effect",
        description: "See website for details\n\n\n(1)     Sending Basic, Binary Switch, Multilevel, and Color SET will automatically disable the strip effect .\n\n(2)     Toggling OFF -> ON will cancel effect mode set on LED Strip and change the LED Strip back to the Previous color.",
        range: "0..2147483647",
        default: 0,
        size: 4,
        type: "number",
        value: null
        ],
    parameter022 : [
        number: 22,
        name: "Custom Strip Effect Parameter 1",
        description: "See website for details.",
        range: "0..2147483647",
        default: 0,
        size: 4,
        type: "number",
        value: null
        ],
    parameter023 : [
        number: 23,
        name: "Custom Strip Effect Parameter 2",
        description: "See website for details.",
        range: "0..2147483647",
        default: 0,
        size: 4,
        type: "number",
        value: null
        ],
    parameter024 : [
        number: 24,
        name: "Custom Strip Effect Parameter 3",
        description: "See website for details.",
        range: "0..2147483647",
        default: 0,
        size: 4,
        type: "number",
        value: null
        ],
    parameter030 : [
        number: 30,
        name: "Custom Strip Effect Parameter 4",
        description: "See website for details.",
        range: "0..32768",
        default: 0,
        size: 4,
        type: "number",
        value: null
        ],
    parameter031 : [
        number: 31,
        name: "Pixel Effect",
        description: "Byte 1 - Level\nByte 0 - Effect",
        range: "0..32768",
        default: 0,
        size: 2,
        type: "number",
        value: null
        ],
    parameter017 : [
        number: 17,
        name: "Active Power Reports",
        description: "The power level change that will result in a new power report being sent. The value is a percentage of the previous report. \n0 = disabled.",
        range: "0..100",
        default: 10,
        size: 1,
        type: "number",
        value: null
        ],
    parameter018 : [
        number: 18,
        name: "Periodic Power & Energy Reports",
        description: "Time period between consecutive power & energy reports being sent (in seconds). The timer is reset after each report is sent.",
        range: "0..32767",
        default: 3600,
        size: 2,
        type: "number",
        value: null
        ],
    parameter019 : [
        number: 19,
        name: "Active Energy Reports",
        description: "Energy reports Energy level change which will result in sending a new energy report. Available settings: 0 - energy reports disabled | 1-127 (0.01-1.27 kWh) - report triggering threshold Default setting: 10 (0.1 kWh)",
        range: "0..100",
        default: 10,
        size: 1,
        type: "number",
        value: null
        ],
    parameter051 : [
        number: 51,
        name:"Disable Physical On/Off Delay",
        description: "The 700ms delay that occurs after pressing the physical button to turn the switch on/off is removed. Consequently this also removes the following scenes: 2x, 3x, 4x, 5x tap. Still working are the 1x tap, held, released, and the level up/down scenes. (firmware 1.36+)",
        range: ["1":"No (Default)", "0":"Yes"],
        default: 1,
        size: 1,
        type: "enum",
        value: null
        ] 
]

private getCommandClassVersions() {
    [0x20: 1, 0x25: 1, 0x70: 1, 0x98: 1, 0x32: 3, 0x5B: 1]
}

def childExists(ep) {
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith(ep)}
    if (childDevice) 
        return true
    else
        return false
}

private channelNumber(String dni) {
    dni.split("-ep")[-1] as Integer
}

private toggleTiles(number, value) {
   if ((number.toInteger() >= 201 && number.toInteger() <= 205) || (number.toInteger() >= 211 && number.toInteger() <= 215) || (number.toInteger() >= 221 && number.toInteger() <= 225)){
   [201,202,203,204,205,211,212,213,214,215,221,222,223,224,225].each { i ->
       if ("${i}" != number){
           def childDevice = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-ep$i"}
           if (childDevice) {         
                childDevice.sendEvent(name: "switch", value: "off")
           }
       } else {
           def childDevice = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-ep$i"}
           if (childDevice) {         
                childDevice.sendEvent(name: "switch", value: value)
           }
       }
   }
   }
}

def childSetLevel(String dni, value) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: childSetLevel($dni, $value)"
    state.lastRan = now()
    def valueaux = value as Integer
    def level = Math.max(Math.min(valueaux, 99), 0)    
    def cmds = []
    switch (channelNumber(dni)) {
        case 9:
            cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: channelNumber(dni), size: 1)
            cmds << zwave.configurationV1.configurationGet(parameterNumber: channelNumber(dni))
        break
        case 10:
            cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: channelNumber(dni), size: 1)
            cmds << zwave.configurationV1.configurationGet(parameterNumber: channelNumber(dni))
        break
        case 101:
            cmds << zwave.protectionV2.protectionSet(localProtectionState : level > 0 ? 1 : 0, rfProtectionState: state.rfProtectionState? state.rfProtectionState:0)
            cmds << zwave.protectionV2.protectionGet()
        break
        case 102:
            cmds << zwave.protectionV2.protectionSet(localProtectionState : state.localProtectionState? state.localProtectionState:0, rfProtectionState : level > 0 ? 1 : 0)
            cmds << zwave.protectionV2.protectionGet()
        break
        case 103:
            cmds << setParameter(14, Math.round(level/10), 1)
            cmds << getParameter(14)
        break
    }
    return commands(cmds)
}

def childOn(String dni) {
    if (infoEnable != "false") log.info "${device.displayName}: childOn($dni)"
    state.lastRan = now()
    def cmds = []
    switch (channelNumber(dni).toInteger()) {
        case 201:
        case 202:
        case 203:
        case 204:
        case 205:
            toggleTiles("${channelNumber(dni)}", "on")
            cmds << setParameter(21, calculateParameter("21-${channelNumber(dni)}"), 4)
            cmds << getParameter(21)
        break
        case 211:
        case 212:
        case 213:
        case 214:
        case 215:
            toggleTiles("${channelNumber(dni)}", "on")
            def actions = settings."pixelEffectString${channelNumber(dni)}".toString().split(",")
            cmds << generatePixelEffectCmds(actions[0].toInteger(), actions[1].toInteger())
        break
        case 221:
        case 222:
        case 223:
        case 224:
        case 225:
            toggleTiles("${channelNumber(dni)}", "on")
            cmds += generateCustomEffectCmds(settings."customEffectString${channelNumber(dni)}")
        break
        default:
            childSetLevel(dni, 99)
        break
    }
    return commands(cmds)
}

def childOff(String dni) {
    if (infoEnable != "false") log.info "${device.displayName}: childOff($dni)"
    state.lastRan = now()
    def cmds = []
    switch (channelNumber(dni).toInteger()) {
        case 201:
        case 202:
        case 203:
        case 204:
        case 205:
            toggleTiles("${channelNumber(dni)}", "off")
            cmds << setParameter(21, 0, 4)
        break
        case 211:
        case 212:
        case 213:
        case 214:
        case 215:
            toggleTiles("${channelNumber(dni)}", "off")
            cmds << setParameter(31, 0, 2)
        break
        case 221:
        case 222:
        case 223:
        case 224:
        case 225:
            toggleTiles("${channelNumber(dni)}", "off")
            cmds << setParameter(30, 0, 4)
        break
        default:
            childSetLevel(dni, 0)
        break
    }
    return commands(cmds)
}

void childRefresh(String dni) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: childRefresh($dni)"
}

def componentSetLevel(cd,level,transitionTime = null) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: componentSetLevel($cd, $value)"
    return childSetLevel(cd.deviceNetworkId,level)
}

def componentOn(cd) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: componentOn($cd)"
    return childOn(cd.deviceNetworkId)
}

def componentOff(cd) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: componentOff($cd)"
    return childOff(cd.deviceNetworkId)
}

void componentRefresh(cd) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: componentRefresh($cd)"
}


private addChild(id, label, namespace, driver, isComponent){
    if(!childExists(id)){
        try {
            def newChild = addChildDevice(namespace, driver, "${device.deviceNetworkId}-${id}", null,
                    [completedSetup: true, label: "${device.displayName} (${label})",
                    isComponent: isComponent])
            newChild.sendEvent(name:"switch", value:"off")
        } catch (e) {
            runIn(3, "sendAlert", [data: [message: "Child device creation failed. Make sure the driver for \"${driver}\" with a namespace of ${namespace} is installed"]])
        }
    }
}

private deleteChild(id){
    if(childExists(id)){
        def childDevice = childDevices.find{it.deviceNetworkId.endsWith(id)}
        try {
            if(childDevice) deleteChildDevice(childDevice.deviceNetworkId)
        } catch (e) {
            if (infoEnable != "false") log.info "There may be issues trying to delete the child device when it is in use. Need to manually delete them."
            runIn(3, "sendAlert", [data: [message: "Failed to delete child device. Make sure the device is not in use by any App."]])
        }
    }
}

def initialize() {
    sendEvent(name: "numberOfButtons", value: 7, displayed: false)
    
    if (enableDefaultLocalChild == "true") addChild("ep112", "Default Local Level", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep112")
    if (enableDefaultZWaveChild == "true") addChild("ep113", "Default Z-Wave Level", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep113")
    if (enableDisableLocalChild == "true") addChild("ep151", "Disable Local Control", "smartthings", "Child Switch", false)
    else deleteChild("ep151")
    if (enableDisableRemoteChild == "true") addChild("ep152", "Disable Remote Control", "smartthings", "Child Switch", false)
    else deleteChild("ep152")
    
    [201,202,203,204,205].each { i ->
        if ((settings."parameter21-${i}a"!=null && settings."parameter21-${i}b"!=null && settings."parameter21-${i}c"!=null && settings."parameter21-${i}d"!=null && settings."parameter21-${i}d"!="0") && !childExists("ep${i}")) {
            addChild("ep${i}", "Quick Effect ${i-200}", "smartthings", "Child Switch", false)
        } else if ((settings."parameter21-${i}a"==null || settings."parameter21-${i}b"==null || settings."parameter21-${i}c"==null || settings."parameter21-${i}d"==null || settings."parameter21-${i}d"=="0") && childExists("ep${i}")) {
            deleteChild("ep${i}")
        }
    }
    
    [211,212,213,214,215].each { i ->
        if (settings."pixelEffectString${i}"!=null && !childExists("ep${i}")) {
            addChild("ep${i}", "Pixel Effect ${i-210}", "smartthings", "Child Switch", false)
        } else if (settings."pixelEffectString${i}"==null && childExists("ep${i}")) {
            deleteChild("ep${i}")
        }
    }
    
    [221,222,223,224,225].each { i ->
        if (settings."customEffectString${i}"!=null && !childExists("ep${i}")) {
            addChild("ep${i}", "Custom Effect ${i-220}", "smartthings", "Child Switch", false)
        } else if (settings."customEffectString${i}"==null && childExists("ep${i}")) {
            deleteChild("ep${i}")
        }
    }
    
    if (device.displayName != state.oldLabel) {
        def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep101")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Disable Local Control)")
        childDevice.setLabel("${device.displayName} (Disable Local Control)")
        childDevice = children.find{it.deviceNetworkId.endsWith("ep102")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Disable Remote Control)")
        childDevice.setLabel("${device.displayName} (Disable Remote Control)")
    }
    
    state.oldLabel = device.displayName
    
    def cmds = processAssociations()
    
    getParameterNumbers().each{ i ->
      if ((state."parameter${i}value" != ((settings."parameter${i}"!=null||calculateParameter(i)!=null)? calculateParameter(i).toInteger() : configParams["parameter${i.toString().padLeft(3,"0")}"].default.toInteger()))){
          //if (infoEnable != "false") log.info "Parameter $i is not set correctly. Setting it to ${settings."parameter${i}"!=null? calculateParameter(i).toInteger() : getParameterInfo(i, "default").toInteger()}."
          cmds << setParameter(i, (settings."parameter${i}"!=null||calculateParameter(i)!=null)? calculateParameter(i).toInteger() : configParams["parameter${i.toString().padLeft(3,"0")}"].default.toInteger(), configParams["parameter${i.toString().padLeft(3,"0")}"].size)
          cmds << getParameter(i)
      }
      else {
          //if (infoEnable != "false") log.info "${device.displayName}: Parameter $i already set"
      }
    }
    
    cmds << zwave.versionV1.versionGet()

    if (cmds != []) return cmds else return []
}

def calculateParameter(number) {
    def value = 0
    switch (number){
      case "21-201":
      case "21-202":
      case "21-203": 
      case "21-204":
      case "21-205":
        if (settings."parameter${number}a".toInteger() < 2700) {
         value += settings."parameter${number}a"!=null ? settings."parameter${number}a".toInteger() * 1 : 0
        } else {
                value += settings."parameter${number}a"!=null ? Math.round((settings."parameter${number}a".toInteger() - 2700) / (6500 - 2700)) * 1 : 0
        }
         value += settings."parameter${number}b"!=null ? (settings."parameter${number}b".toInteger() + 128) * 256 : 0
         value += settings."parameter${number}c"!=null ? settings."parameter${number}c".toInteger() * 65536 : 0
        if (settings."parameter${number}a".toInteger() < 2700) {
         value += settings."parameter${number}d"!=null ? settings."parameter${number}d".toInteger() * 16777216 : 0
        } else {
                value += settings."parameter${number}d"!=null ? (settings."parameter${number}d".toInteger() + 64) * 16777216 : 0
        }
      break
      case "9":
        if (settings."parameter${number}" >= 0 && settings."parameter${number}" <= 360) {
            value += settings."parameter${number}".toInteger() * 1
        } else {
            value += settings."parameter${number}"!=null ? settings."parameter${number}".toInteger() * 65536 : 2700 * 65536
        }
      break
      default:
          value = settings."parameter${number}"
      break
    }
    return value
}

def getParameterNumbers(){
    return [1,2,3,4,5,6,7,8,9,10,17,18,19,51]
}

def setConfigParameter(number, value, size) {
    return command(setParameter(number, value, size.toInteger()))
}

def generateCustomEffectCmds(effect){
    def actions = effect.split(',')
    return [setParameter(22, actions[1-1], 4),
                     setParameter(23, actions[2-1], 4),
                     setParameter(24, actions[3-1], 4),
                     setParameter(30, actions[4-1], 4),
                     getParameter(22),
                     getParameter(23),
                     getParameter(24),
                     getParameter(30)
                    ]
}

def customEffectStart(effect) {
    return commands(generateCustomEffectCmds(effect))
}

def customEffectStop() {
    return commands([setParameter(30, 0, 4),
                     getParameter(30)
                    ]
                   )
}


def setParameter(number, value, size) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Setting parameter $number with a size of $size bytes to $value"
    return zwave.configurationV1.configurationSet(scaledConfigurationValue: value.toInteger(), parameterNumber: number, size: size)
}

def getParameter(number) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Retreiving value of parameter $number"
    return zwave.configurationV1.configurationGet(parameterNumber: number)
}

def debugLogsOff(){
    log.warn "${device.label?device.label:device.name}: Disabling debug logging after timeout"
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

def infoLogsOff(){
    log.warn "${device.label?device.label:device.name}: Disabling info logging after timeout"
    device.updateSetting("infoEnable",[value:"false",type:"bool"])
}

def startNotification(value, ep = null){
    if (infoEnable != "false") log.info "${device.displayName}: startNotification($value)"
    def cmds = []
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value.toInteger(), parameterNumber: ledNotificationEndpoints[(ep == null)? 0:ep?.toInteger()-1], size: 4)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: ledNotificationEndpoints[(ep == null)? 0:ep?.toInteger()-1])
    return commands(cmds)
}

def stopNotification(ep = null){
    if (infoEnable != "false") log.info "${device.displayName}: stopNotification()"
    def cmds = []
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: 0, parameterNumber: ledNotificationEndpoints[(ep == null)? 0:ep?.toInteger()-1], size: 4)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: ledNotificationEndpoints[(ep == null)? 0:ep?.toInteger()-1])
    return commands(cmds)
}

def generatePixelEffectCmds(number, level = 99){
    def cmdValue = 0
    if (number > maxPixelEffects) number = 1
    if (number < 1) number = maxPixelEffects
    if (level > 99) level = 99
    if (level < 1) level = 99
    state.pixelEffectNumber = number
    state.pixelEffectLevel = level
    cmdValue += number
    cmdValue += level * 256
    return setParameter(31, cmdValue, 2)
}

def pixelEffectStart(number, level = 99) {
    return command(generatePixelEffectCmds(number, level = 99))
}

def pixelEffectNext(){
    return pixelEffectStart(state.pixelEffectNumber? state.pixelEffectNumber + 1 : 1, state.pixelEffectLevel)
}

def pixelEffectPrevious(){
    return pixelEffectStart(state.pixelEffectNumber? state.pixelEffectNumber - 1 : 1, state.pixelEffectLevel)
}

def pixelEffectStop(){
    return command(setParameter(31, 0, 2))
}

private huePercentToValue(value){
    return value<=2?0:(value>=98?360:value/100*360)
}

private hueValueToZwaveValue(value){
    return value<=2?0:(value>=356?255:value/360*255)
}

private huePercentToZwaveValue(value){
    return value<=2?0:(value>=98?254:value/100*255)
}

private zwaveValueToHueValue(value){
    return value<=2?0:(value>=254?360:value/255*360)
}

private zwaveValueToHuePercent(value){
    return value<=2?0:(value>=254?100:value/255*100)
}

def installed() {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: installed()"
    refresh()
}

def configure() {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: configure()"
    def cmds = initialize()
    commands(cmds)
}

def updated() {
    if (!state.lastRan || now() >= state.lastRan + 2000) {
        if (infoEnable != "false") log.info "${device.label?device.label:device.name}: updated()"
        if (debugEnable && disableDebugLogging) runIn(disableDebugLogging*60,debugLogsOff)
        if (infoEnable && disableInfoLogging) runIn(disableInfoLogging*60,infoLogsOff)
        state.lastRan = now()
        def cmds = initialize()
        if (cmds != [])
            response(commands(cmds, 1000))
        else 
            return null
    } else {
        if (infoEnable != "false") log.info "${device.label?device.label:device.name}: updated() ran within the last 2 seconds. Skipping execution."
    }
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    def event
    if (cmd.scale == 0) {
        if (cmd.meterType == 161) {
            event = createEvent(name: "voltage", value: cmd.scaledMeterValue, unit: "V")
            if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Voltage report received with value of ${cmd.scaledMeterValue} V"
        } else if (cmd.meterType == 1) {
            event = createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
            if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Energy report received with value of ${cmd.scaledMeterValue} kWh"
        }
    } else if (cmd.scale == 1) {
        event = createEvent(name: "amperage", value: cmd.scaledMeterValue, unit: "A")
        if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Amperage report received with value of ${cmd.scaledMeterValue} A"
    } else if (cmd.scale == 2) {
        event = createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W")
        if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Power report received with value of ${cmd.scaledMeterValue} W"
    }

    return event
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd.scaledConfigurationValue}'"
    def integerValue = cmd.scaledConfigurationValue
    state."parameter${cmd.parameterNumber}value" = integerValue
    /*switch (cmd.parameterNumber) {
        case 9:
            device.updateSetting("parameter${cmd.parameterNumber}",[value:integerValue,type:"number"])
            def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep9")}
            if (childDevice) {
            childDevice.sendEvent(name: "switch", value: integerValue > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: integerValue)            
            }
        break
        case 10:
            device.updateSetting("parameter${cmd.parameterNumber}",[value:integerValue,type:"number"])
            def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep10")}
            if (childDevice) {
            childDevice.sendEvent(name: "switch", value: integerValue > 0 ? "on" : "off")
            childDevice.sendEvent(name: "level", value: integerValue)
            }
        break
    }*/
}

def refresh() {
    commands([zwave.switchMultilevelV2.switchMultilevelGet()] + queryAllColors())
}

private void refreshColor() {
    sendToDevice(queryAllColors())
}

def startLevelChange(direction) {
    def upDownVal = direction == "down" ? true : false
    if (infoEnable != "false") log.debug "${device.label?device.label:device.name}: startLevelChange(${direction})"
    commands([zwave.switchMultilevelV2.switchMultilevelStartLevelChange(ignoreStartLevel: true, startLevel: device.currentValue("level"), upDown: upDownVal)])
}

def stopLevelChange() {
    if (infoEnable != "false") log.debug "${device.label?device.label:device.name}: stopLevelChange()"
    commands([zwave.switchMultilevelV2.switchMultilevelStopLevelChange()])
}

void zwaveEvent(physicalgraph.zwave.commands.switchcolorv2.SwitchColorReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"

}

def on() {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: on()"
    state.lastRan = now()
    commands([
        zwave.basicV1.basicSet(value: 0xFF)
    ])
}

def off() {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: off()"
    state.lastRan = now()
    commands([
        zwave.basicV1.basicSet(value: 0x00)
    ])
}

def setLevel(value) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: setLevel($value)"
    state.lastRan = now()
    commands([
        zwave.switchMultilevelV2.switchMultilevelSet(value: value < 100 ? value : 99),
        zwave.switchMultilevelV2.switchMultilevelGet()
    ], 4000)
}

def setLevel(value, duration) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: setLevel($value, $duration)"
    state.lastRan = now()
    def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
    commands([
        zwave.switchMultilevelV2.switchMultilevelSet(value: value < 100 ? value : 99, dimmingDuration: dimmingDuration),
        zwave.switchMultilevelV2.switchMultilevelGet()
    ], 4000)
}

void setSaturation(percent) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: setSaturation($percent)"
    setColor([saturation: percent, hue: device.currentValue("hue"), level: device.currentValue("level")])
}

void setHue(value) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: setHue($value)"
    setColor([hue: value, saturation: 100, level: device.currentValue("level")])
}

def setColor(value) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: setColor($value)"
    def cmds = []
    if (value.hex) {
        def c = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
        cmds << zwave.switchColorV2.switchColorSet(red: c[0], green: c[1], blue: c[2], warmWhite: 0, coldWhite: 0)
    } else {
        def rgb = huesatToRGB([value.hue, value.saturation])
        cmds << zwave.switchColorV2.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2], warmWhite:0, coldWhite:0)
    }
    if ((device.currentValue("switch") != "on") && (!colorStaging)){
        if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Bulb is off. Turning on"
        cmds << zwave.basicV1.basicSet(value: 0xFF)
    }
    sendEvent(name: "colorMode", value: "RGB", descriptionText: "${device.getDisplayName()} color mode is RGB")
    sendEvent(name: "hue", value: value.hue)
    sendEvent(name: "saturation", value: value.saturation)
    commands(cmds)// + "delay 4000" + commands(queryAllColors(), 500)
}

def setColorTemperature(temp) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: setColorTemperature($temp)"
    def cmds = []
    if (temp < COLOR_TEMP_MIN) temp = COLOR_TEMP_MIN
    if (temp > COLOR_TEMP_MAX) temp = COLOR_TEMP_MAX
    def warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF * 255) as Integer
    def coldValue = 255 - warmValue
    cmds << zwave.switchColorV2.switchColorSet(red: 0, green: 0, blue: 0, warmWhite: warmValue, coldWhite: coldValue)
    if ((device.currentValue("switch") != "on") && (!colorStaging)) {
        if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Bulb is off. Turning on"
        cmds << zwave.basicV1.basicSet(value: 0xFF)
    }
    sendEvent(name: "colorMode", value: "CT", descriptionText: "${device.getDisplayName()} color mode is CT")
    sendEvent(name: "colorTemperature", value: temp)
    commands(cmds)// + "delay 4000" + commands(queryAllColors(), 500)
}

def rgbToHSV(red, green, blue) {
    def hex = colorUtil.rgbToHex(red as int, green as int, blue as int)
    def hsv = colorUtil.hexToHsv(hex)
    return [hue: hsv[0], saturation: hsv[1], value: hsv[2]]
}

def huesatToRGB(hue, sat) {
    def color = colorUtil.hsvToHex(Math.round(hue) as int, Math.round(sat) as int)
    return colorUtil.hexToRgb(color)
}

private queryAllColors() {
    def colors = WHITE_NAMES + RGB_NAMES
    [zwave.basicV1.basicGet()] + colors.collect { zwave.switchColorV2.switchColorGet(colorComponent: it) }
}

void zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    physicalgraph.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
}

def parse(description) {
    def result = null
    if (description.startsWith("Err 106")) {
        state.sec = 0
        result = createEvent(descriptionText: description, isStateChange: true)
    } else if (description != "updated") {
        def cmd = zwave.parse(description, commandClassVersions)
        if (cmd) {
            result = zwaveEvent(cmd)
            //log.debug("'$cmd' parsed to $result")
        } else {
            if (debugEnable != "false") log.debug "Couldn't zwave.parse '$description'" 
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
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Basic report received with value of ${cmd.value ? "on" : "off"} ($cmd.value)"
    dimmerEvents(cmd, (!state.lastRan || now() <= state.lastRan + 2000)?"digital":"physical")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Basic set received with value of ${cmd.value ? "on" : "off"}"
    dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Switch Binary report received with value of ${cmd.value ? "on" : "off"}"
    dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Switch Multilevel report received with value of ${cmd.value ? "on" : "off"} ($cmd.value)"
    dimmerEvents(cmd, (!state.lastRan || now() <= state.lastRan + 2000)?"digital":"physical")
}

private dimmerEvents(physicalgraph.zwave.Command cmd, type="physical") {
    def value = (cmd.value ? "on" : "off")
    def result = [createEvent(name: "switch", value: value, type: type)]
    if (cmd.value) {
        result << createEvent(name: "level", value: cmd.value, unit: "%", type: type)
    }
    return result
}

void zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    switch (cmd.keyAttributes) {
       case 0:
       if (cmd.sceneNumber == 1) buttonEvent(7, "pushed", "physical")
       else buttonEvent(cmd.keyAttributes + 1, (cmd.sceneNumber == 2? "pushed" : "held"), "physical")
       break
       default:
       buttonEvent(cmd.keyAttributes - 1, (cmd.sceneNumber == 2? "pushed" : "held"), "physical")
       break
    }
}

void buttonEvent(button, value, type = "digital") {
    if(button != 6)
        sendEvent(name:"lastEvent", value: "${value != 'pushed'?' Tap '.padRight(button+5, '▼'):' Tap '.padRight(button+5, '▲')}", displayed:false)
    else
        sendEvent(name:"lastEvent", value: "${value != 'pushed'?' Hold ▼':' Hold ▲'}", displayed:false)
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Button ${button} was ${value}"
    
    sendEvent([name: "button", value: value, data: [buttonNumber: button], descriptionText: "$device.displayName button $button was $value", isStateChange: true, type: type])
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: Unhandled: $cmd"
    null
}

def reset() {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Resetting energy statistics"
    def cmds = []
    cmds << zwave.meterV2.meterReset()
    cmds << zwave.meterV2.meterGet(scale: 0)
    cmds << zwave.meterV2.meterGet(scale: 2)
    commands(cmds, 1000)
}

private command(physicalgraph.zwave.Command cmd) {
    if (getZwaveInfo()?.zw?.contains("s")) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay = 250) {
    delayBetween(commands.collect {
        command(it)
    }, delay)
}

private int gammaCorrect(value) {
    def temp=value/255
    def correctedValue=(temp>0.4045) ? Math.pow((temp+0.055)/ 1.055, 2.4) : (temp / 12.92)
    return Math.round(correctedValue * 255) as Integer
}

def setDefaultAssociations() {
    def smartThingsHubID = String.format('%02x', zwaveHubNodeId).toUpperCase()
    state.defaultG1 = [smartThingsHubID]
    state.defaultG2 = []
    state.defaultG3 = []
}

def setAssociationGroup(group, nodes, action, endpoint = null){
    // Normalize the arguments to be backwards compatible with the old method
    action = "${action}" == "1" ? "Add" : "${action}" == "0" ? "Remove" : "${action}" // convert 1/0 to Add/Remove
    group  = "${group}" =~ /\d+/ ? (group as int) : group                             // convert group to int (if possible)
    nodes  = [] + nodes ?: [nodes]                                                    // convert to collection if not already a collection

    if (! nodes.every { it =~ /[0-9A-F]+/ }) {
        log.error "${device.label?device.label:device.name}: invalid Nodes ${nodes}"
        return
    }

    if (group < 1 || group > maxAssociationGroup()) {
        log.error "${device.label?device.label:device.name}: Association group is invalid 1 <= ${group} <= ${maxAssociationGroup()}"
        return
    }
    
    def associations = state."desiredAssociation${group}"?:[]
    nodes.each { 
        node = "${it}"
        switch (action) {
            case "Remove":
            if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Removing node ${node} from association group ${group}"
            associations = associations - node
            break
            case "Add":
            if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Adding node ${node} to association group ${group}"
            associations << node
            break
        }
    }
    state."desiredAssociation${group}" = associations.unique()
    return
}

def maxAssociationGroup(){
   if (!state.associationGroups) {
       if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Getting supported association groups from device"
       //sendHubCommand(new device.HubAction(command(zwave.associationV2.associationGroupingsGet()), hubitat.device.Protocol.ZWAVE )) // execute the update immediately
   }
   (state.associationGroups?: 5) as int
}

def processAssociations(){
   def cmds = []
   setDefaultAssociations()
   def associationGroups = maxAssociationGroup()
   for (int i = 1; i <= associationGroups; i++){
      if(state."actualAssociation${i}" != null){
         if(state."desiredAssociation${i}" != null || state."defaultG${i}") {
            def refreshGroup = false
            ((state."desiredAssociation${i}"? state."desiredAssociation${i}" : [] + state."defaultG${i}") - state."actualAssociation${i}").each {
                if (it){
                    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Adding node $it to group $i"
                    cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId: Integer.parseInt(it,16))
                    refreshGroup = true
                }
            }
            ((state."actualAssociation${i}" - state."defaultG${i}") - state."desiredAssociation${i}").each {
                if (it){
                    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Removing node $it from group $i"
                    cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId: Integer.parseInt(it,16))
                    refreshGroup = true
                }
            }
            if (refreshGroup == true) cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
            else if (infoEnable != "false") log.info "${device.label?device.label:device.name}: There are no association actions to complete for group $i"
         }
      } else {
         if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Association info not known for group $i. Requesting info from device."
         cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
      }
   }
   return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    def temp = []
    if (cmd.nodeId != []) {
       cmd.nodeId.each {
          temp += it.toString().format( '%02x', it.toInteger() ).toUpperCase()
       }
    } 
    state."actualAssociation${cmd.groupingIdentifier}" = temp
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Associations for Group ${cmd.groupingIdentifier}: ${temp}"
    updateDataValue("associationGroup${cmd.groupingIdentifier}", "$temp")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${cmd}"
    if(cmd.applicationVersion != null && cmd.applicationSubVersion != null) {
        def firmware = "${cmd.applicationVersion}.${cmd.applicationSubVersion.toString().padLeft(2,'0')}"
        if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Firmware report received: ${firmware}"
        state.needfwUpdate = "false"
        createEvent(name: "firmware", value: "${firmware}")
    } else if(cmd.firmware0Version != null && cmd.firmware0SubVersion != null) {
        def firmware = "${cmd.firmware0Version}.${cmd.firmware0SubVersion.toString().padLeft(2,'0')}"
        if (infoEnable != false) log.info "${device.label?device.label:device.name}: Firmware report received: ${firmware}"
        state.needfwUpdate = "false"
        createEvent(name: "firmware", value: "${firmware}")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.protectionv2.ProtectionReport cmd) {
    if (debugEnable != "false") log.debug "${device.label?device.label:device.name}: ${device.label?device.label:device.name}: ${cmd}"
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: Protection report received: Local protection is ${cmd.localProtectionState > 0 ? "on" : "off"} & Remote protection is ${cmd.rfProtectionState > 0 ? "on" : "off"}"
    state.localProtectionState = cmd.localProtectionState
    state.rfProtectionState = cmd.rfProtectionState
    device.updateSetting("disableLocal",[value:cmd.localProtectionState?"1":"0",type:"enum"])
    device.updateSetting("disableRemote",[value:cmd.rfProtectionState?"1":"0",type:"enum"])
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith("ep101")}
    if (childDevice) {
        childDevice.sendEvent(name: "switch", value: cmd.localProtectionState > 0 ? "on" : "off")        
    }
    childDevice = children.find{it.deviceNetworkId.endsWith("ep102")}
    if (childDevice) {
        childDevice.sendEvent(name: "switch", value: cmd.rfProtectionState > 0 ? "on" : "off")        
    }
}
