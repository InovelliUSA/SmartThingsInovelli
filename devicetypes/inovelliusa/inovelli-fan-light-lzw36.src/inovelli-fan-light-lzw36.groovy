/**
 *  Inovelli Fan + Light LZW36
 *  Author: Eric Maycock (erocm123)
 *  Date: 2020-08-14
 *
 *  ******************************************************************************************************
 *
 *  REQUIRED CHILD DEVICE HANDLERS:
 *      Fan & Light: Required to create separate devices to control the fan and light individually.
 *        https://raw.githubusercontent.com/InovelliUSA/SmartThingsInovelli/master/devicetypes/inovelliusa/switch-level-child-device.src/switch-level-child-device.groovy
 *  OPTIONAL CHILD DEVICE HANDLERS:
 *      LED Bar for Fan & Light: In the device settings you can enable up to 2 RGBW child devices that can be used to change the color and intesity of the LED bars.
 *        https://raw.githubusercontent.com/InovelliUSA/SmartThingsInovelli/master/devicetypes/inovelliusa/rgbw-child-device.src/rgbw-child-device.groovy
 *
 *  ******************************************************************************************************
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
 *  2020-08-14: Added configuration parameter 51 for firmware 1.36+ 
 *
 *  2020-08-05: Adding cycleSpeed() command for use with fan child device.
 *
 *  2020-08-04: Fixing number of buttons being reported. After update you will need to change a preference to update the value.
 *
 *  2020-08-03: Adding "reset()" to reset the energy accumulation numbers.
 *
 *  2020-07-17: Added child devices for "Light LED Color", "Light LED Color When OFF", "Fan LED Color", "Fan LED Color When OFF"
 *              You need to install the child device below for this to work:
 *              https://raw.githubusercontent.com/InovelliUSA/SmartThingsInovelli/master/devicetypes/inovelliusa/rgb-child-device.src/rgb-child-device.groovy
 *
 *  2020-07-09: Re-write of much of the code to make the handler more efficient. 
 *              Not adding two attributes to child devices upon creation for better compatibility with new app. 
 *
 *  2020-07-02: Adding support for Inovelli Virtual Device Sync (VDS) SmartApp.
 *              This app can be used in place of "child devices" to create virtual devices to 
 *              individually control the fan or light in the new SmartThings app as well as
 *              the new Google Home / Alexa integration.
 *              More Info: https://vds.inovelli.com
 *
 *  2020-06-30: Fixes for local protection settings and some config parameter adjustments.
 *              Fix for renaming of child devices. 
 * 
 */

import groovy.json.JsonOutput
import groovy.transform.Field
 
metadata {
    definition(
        name: "Inovelli Fan + Light LZW36", 
        namespace: "InovelliUSA", 
        author: "Eric Maycock", 
        ocfDeviceType: "oic.d.fan") {
        
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
        capability "Switch Level"
        capability "Light"
        capability "Refresh"
        capability "Health Check"
        capability "Button"
        
        capability "Energy Meter"
        capability "Power Meter"
        capability "Configuration"
        
        attribute "lastActivity", "String"
        attribute "lastEvent", "String"
        attribute "firmware", "String"
        attribute "groups", "Number"
        
        attribute "switch1", "string"
        attribute "switch2", "string"
        attribute "level1", "number"
        attribute "level2", "number"

        command "setAssociationGroup", ["number", "enum", "number", "number"] // group number, nodes, action (0 - remove, 1 - add), multi-channel endpoint (optional)
        command "childOn"
        command "childOff"
        command "childRefresh"
        command "childSetLevel"
        command "setSpeed"
        command "cycleSpeed"
        
        command "on1"
        command "off1"
        command "on2"
        command "off2"
        command "setLevel1"
        command "setLevel2"
        command "reset"

        fingerprint manufacturer: "031E", prod: "000E", model: "0001", deviceJoinName: "Inovelli Fan + Light"
        
        fingerprint deviceId: "0x1100", inClusters: "0x5E,0x55,0x98,0x9F,0x6C,0x26,0x70,0x85,0x59,0x8E,0x86,0x72,0x5A,0x73,0x75,0x22,0x7A,0x5B,0x87,0x60"
        fingerprint deviceId: "0x1100", inClusters: "0x5E,0x55,0x98,0x6C,0x26,0x70,0x85,0x59,0x8E,0x86,0x72,0x5A,0x73,0x75,0x22,0x7A,0x5B,0x87,0x60,0x32"
    }
    
    simulator {}
    
    preferences {
        input "useVDS", "boolean", title: "Use Inovelli Virtual Device Sync\n\nUse the VDS smartapp instead of creating child devices. The app will create virtual devices that can be used in the new SmartThings app and Google Home / Alexa. More Info: https://vds.inovelli.com", description: "", required: false, defaultValue: "false"
        input "parameter1", "number",
            title: "Dimming Speed: This changes the speed in which the attached light dims up or down. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed.\nRange: 0..100\nDefault: 4",
            range: "0..100"
        input "parameter2", "number",
            title: "Dimming Speed (From Switch): This changes the speed in which the attached light dims up or down when controlled from the physical switch. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed. A setting of 99 should keep this in sync with parameter 1.\nRange: 0..99\nDefault: 99",
            range: "0..99"
        input "parameter3", "number",
            title: "Ramp Rate: This changes the speed in which the attached light turns on or off. For example, when a user sends the switch a basicSet(value: 0xFF) or basicSet(value: 0x00), this is the speed in which those actions take place. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed. A setting of 99 should keep this in sync with parameter 1.\nRange: 0..99\nDefault: 99",
            range: "0..99"
        input "parameter4", "number",
            title: "Ramp Rate (From Switch): This changes the speed in which the attached light turns on or off from the physical switch. For example, when a user presses the up or down button, this is the speed in which those actions take place. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed. A setting of 99 should keep this in sync with parameter 1.\nRange: 0..99\nDefault: 99",
            range: "0..99"
        input "parameter5", "number",
            title: "Minimum Light Level: The minimum level that the dimmer allows the bulb to be dimmed to. Useful when the user has a bulb that does not turn on at a lower level.\nRange: 1..45\nDefault: 1",
            range: "1..45"
        input "parameter6", "number",
            title: "Maximum Light Level: The maximum level that the dimmer allows the bulb to be dimmed to. Useful when the user has an LED bulb that reaches its maximum level before the dimmer value of 99.\nRange: 55..99\nDefault: 99",
            range: "55..99"
        input "parameter7", "number",
            title: "Minimum Fan Level: The minimum level that the dimmer allows the fan to be dimmed to. Useful when the user has a fan that does not turn at a lower level.\nRange: 1..45\nDefault: 1",
            range: "1..45"
        input "parameter8", "number",
            title: "Maximum Fan Level: The maximum level that the dimmer allows the fan to be dimmed to.\nRange: 55..99\nDefault: 99",
            range: "55..99"
        input "parameter10", "number",
            title: "Auto Off Light Timer: Automatically turns the light switch off after this many seconds. When the switch is turned on a timer is started that is the duration of this setting. When the timer expires, the switch is turned off.\nRange: 0..32767\nDefault: 0",
            range: "0..32767"
        input "parameter11", "number",
            title: "Auto Off Fan Timer: Automatically turns the fan switch off after this many seconds. When the switch is turned on a timer is started that is the duration of this setting. When the timer expires, the switch is turned off.\nRange: 0..32767\nDefault: 0",
            range: "0..32767"
        input "parameter12", "number",
            title: "Default Light Level (Local): Default level for the dimmer when it is powered on from the local switch. A setting of 0 means that the switch will return to the level that it was on before it was turned off.\nRange: 0..99\nDefault: 0",
            range: "0..99"
        input "parameter13", "number",
            title: "Default Light Level (Z-Wave): Default level for the dimmer when it is powered on from a Z-Wave command. A setting of 0 means that the switch will return to the level that it was on before it was turned off.\nRange: 0..99\nDefault: 0",
            range: "0..99"
        input "parameter14", "number",
            title: "Default Fan Level (Local): Default level for the dimmer when it is powered on from the local switch. A setting of 0 means that the switch will return to the level that it was on before it was turned off.\nRange: 0..99\nDefault: 0",
            range: "0..99"
        input "parameter15", "number",
            title: "Default Fan Level (Z-Wave): Default level for the dimmer when it is powered on from a Z-Wave command. A setting of 0 means that the switch will return to the level that it was on before it was turned off.\nRange: 0..99\nDefault: 0",
            range: "0..99"
        input "parameter16", "number",
            title: "Light State After Power Restored: The state the switch should return to once power is restored after power failure. 0 = off, 1-99 = level, 100=previous.\nRange: 0..100\nDefault: 0",
            range: "0..100"
        input "parameter17", "number",
            title: "Fan State After Power Restored: The state the switch should return to once power is restored after power failure. 0 = off, 1-99 = level, 100=previous.\nRange: 0..100\nDefault: 0",
            range: "0..100"
        input "parameter18", "enum",
            title: "Light LED Indicator Color: This is the color of the LED strip for the Light.\nDefault: Blue",
            options: ["0":"Red", "21":"Orange", "42":"Yellow", "85":"Green", "127":"Cyan", "170":"Blue", "212":"Violet", "234":"Pink", "255":"White"]
        input "parameter19", "enum",
            title: "Light LED Strip Intensity: This is the intensity of the Light LED strip.\nDefault: 50%",
            options: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"]
        input "parameter20", "enum",
            title: "Fan LED Indicator Color: This is the color of the LED strip for the Fan.\nDefault: Blue",
            options: ["0":"Red", "21":"Orange", "42":"Yellow", "85":"Green", "127":"Cyan", "170":"Blue", "212":"Violet", "234":"Pink", "255":"White"]
        input "parameter21", "enum",
            title: "Fan LED Strip Intensity: This is the intensity of the Fan LED strip.\nDefault: 50%",
            options: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"]
        input "parameter22", "enum",
            title: "Light LED Strip Intensity (When OFF): This is the intensity of the Light LED strip when the switch is off. This is useful for users to see the light switch location when the lights are off.\nDefault: 10%",
            options: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"]
        input "parameter23", "enum",
            title: "Fan LED Strip Intensity (When OFF): This is the intensity of the Fan LED strip when the switch is off. This is useful for users to see the light switch location when the lights are off.\nDefault: 10%",
            options: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"]
        input "parameter26", "enum",
            title: "Light LED Strip Timeout: When the LED strip is disabled (Light LED Strip Intensity is set to 0), this setting allows the LED strip to turn on temporarily while being adjusted.\nDefault: 3 Seconds",
            options: ["0":"Stay Off", "1":"1 Second", "2":"2 Seconds", "3":"3 Seconds", "4":"4 Seconds", "5":"5 Seconds", "6":"6 Seconds", "7":"7 Seconds", "8":"8 Seconds", "9":"9 Seconds", "10":"10 Seconds"]
        input "parameter27", "enum",
            title: "Fan LED Strip Timeout: When the LED strip is disabled (Fan LED Strip Intensity is set to 0), this setting allows the LED strip to turn on temporarily while being adjusted.\nDefault: 3 Seconds",
            options: ["0":"Stay Off", "1":"1 Second", "2":"2 Seconds", "3":"3 Seconds", "4":"4 Seconds", "5":"5 Seconds", "6":"6 Seconds", "7":"7 Seconds", "8":"8 Seconds", "9":"9 Seconds", "10":"10 Seconds"]
        input "parameter28", "number",
            title: "Active Power Reports: The power level change that will result in a new power report being sent. The value is a percentage of the previous report. 0 = disabled.\nRange: 0..100\nDefault: 10",
            range: "0..100"
        input "parameter29", "number",
            title: "Periodic Power & Energy Reports: Time period between consecutive power & energy reports being sent (in seconds). The timer is reset after each report is sent.\nRange: 0..32767\nDefault: 3600",
            range: "0..32767"
        input "parameter30", "number",
            title: "Energy Reports: The energy level change that will result in a new energy report being sent. The value is a percentage of the previous report.\nRange: 0..100\nDefault: 10",
            range: "0..100"
        input "parameter31", "enum",
            title: "Local Protection Settings: Enable local protection on these buttons.\nDefault: None",
            options: ["0":"None", "1":"Light", "2":"Fan", "3":"Both"]
        input "parameter51", "enum",
            title: "Disable Physical On/Off Delay: The 700ms delay that occurs after pressing the physical button to turn the switch on/off is removed. Consequently this also removes the following scenes: 2x, 3x, 4x, 5x tap. Still working are the 1x tap, held, released, and the level up/down scenes. (firmware 1.36+).\nDefault: No",
            options: ["1":"No (Default)", "0":"Yes"]
        
        [201,202,203,204,205].each { i ->
            input "parameter24-${i}a", "enum", title: "Light Notification ${i-200} - Color", description: "Tap to set", 
                displayDuringSetup: false, required: false, options: [
                1:"Red", 21:"Orange", 42:"Yellow", 85:"Green", 127:"Cyan", 170:"Blue", 212:"Violet", 234:"Pink", 255:"White"]
            input "parameter24-${i}b", "enum", title: "Light Notification ${i-200} - Level", description: "Tap to set", 
                displayDuringSetup: false, required: false, options: [
                0:"0%", 1:"10%", 2:"20%", 3:"30%", 4:"40%", 5:"50%", 6:"60%", 7:"70%", 8:"80%", 9:"90%", 10:"100%"]
            input "parameter24-${i}c", "enum", title: "Light Notification ${i-200} - Duration", description: "Tap to set", 
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
            input "parameter24-${i}d", "enum", title: "Light Notification ${i-200} - Effect", description: "Tap to set", displayDuringSetup: false, required: false, options: [
                0:"Off", 1:"Solid", 2:"Chase", 3:"Fast Blink", 4:"Slow Blink", 5:"Pulse"]
        }
        [251,252,253,254,255].each { i ->
            input "parameter25-${i}a", "enum", title: "Fan Notification ${i-250} - Color", description: "Tap to set", 
                displayDuringSetup: false, required: false, options: [
                1:"Red", 21:"Orange", 42:"Yellow", 85:"Green", 127:"Cyan", 170:"Blue", 212:"Violet", 234:"Pink", 255:"White"]
            input "parameter25-${i}b", "enum", title: "Fan Notification ${i-250} - Level", description: "Tap to set", 
                displayDuringSetup: false, required: false, options: [
                0:"0%", 1:"10%", 2:"20%", 3:"30%", 4:"40%", 5:"50%", 6:"60%", 7:"70%", 8:"80%", 9:"90%", 10:"100%"]
            input "parameter25-${i}c", "enum", title: "Fan Notification ${i-250} - Duration", description: "Tap to set", 
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
            input "parameter25-${i}d", "enum", title: "Fan Notification ${i-250} - Effect", description: "Tap to set", displayDuringSetup: false, 
                required: false, options: [
                    0:"Off", 1:"Solid", 2:"Chase", 3:"Fast Blink", 4:"Slow Blink", 5:"Pulse"]
        }
        input description: "Use the options below to setup associations. Associations can be used to directly control one z-wave device from another.",
            title: "Associations", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input "group2e1", "string",
            title: "Multi-channel Group 2 Endpoint 1: Sends On/Off commands when you press the light up or light down button. This should be a device id hex value of the device you are wanting to control."
        input "group4e1", "string",
            title: "Multi-channel Group 4 Endpoint 1: Sends level up/down commands when you hold the light up or light down button. This should be a device id hex value of the device you are wanting to control."
        input "group5e1", "string",
            title: "Multi-channel Group 5 Endpoint 2: Sends On/Off commands when you press the light up or light down button. This should be a device id hex value of the device you are wanting to control."
        input "group7e1", "string",
            title: "Multi-channel Group 7 Endpoint 2: Sends level up/down commands when you hold the light up or light down button. This should be a device id hex value of the device you are wanting to control."
        input description: "Use the below options to enable child devices for the specified settings. This will allow you to adjust these settings using " +
            "SmartApps such as Smart Lighting.", title: "Child Devices", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input "enableDefaultLocalChild", "boolean", title: "Create \"Default Level (Local)\" Child Device", description: "", required: false, defaultValue: "false"
        input "enableDefaultZWaveChild", "boolean", title: "Create \"Default Level (Z-Wave)\" Child Device", description: "", required: false, defaultValue: "false"
        input "enableLED1Child", "boolean", title: "Create \"Light LED\" Child Device", description: "", required: false, defaultValue: "false"
        input "enableLED1OffChild", "boolean", title: "Create \"Light LED When Off\" Child Device", description: "", required: false, defaultValue: "false"
        input "enableLED2Child", "boolean", title: "Create \"Fan LED\" Child Device", description: "", required: false, defaultValue: "false"
        input "enableLED2OffChild", "boolean", title: "Create \"Fan LED When Off\" Child Device", description: "", required: false, defaultValue: "false"
        input description: "Use the options below to enable or disable logs in case they are needed to provide addtional information on how the device is functioning.",
            title: "Log Settings", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input name: "debugEnable", type: "boolean", 
            title: "Enable Debug Logging", defaultValue: true
        input name: "infoEnable", type: "boolean", 
            title: "Enable Informational Logging", defaultValue: true
        input name: "disableDebugLogging", type: "number", 
            title: "Disable Debug Logging: Disable debug logging after this number of minutes (0=Do not disable)", defaultValue: 0
        input name: "disableInfoLogging", type: "number", 
            title: "Disable Info Logging: Disable info logging after this number of minutes (0=Do not disable)", defaultValue: 30
    }
    
    tiles {
        multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
                attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
            tileAttribute("device.lastEvent", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'${currentValue}',icon: "st.unknown.zwave.remote-controller")
            }
            
        }
        childDeviceTiles("all")
        valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue} W'
		}
		valueTile("energy", "device.energy", decoration: "flat", width: 2, height: 2) {
			state "default", label:'${currentValue} kWh'
		}
        valueTile("firmware", "device.firmware", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'fw: ${currentValue}', icon: ""
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        valueTile("lastActivity", "device.lastActivity", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label: 'Last Activity: ${currentValue}',icon: "st.Health & Wellness.health9"
        }
        valueTile("icon", "device.icon", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label: '', icon: "https://inovelli.com/wp-content/uploads/Device-Handler/Inovelli-Device-Handler-Logo.png"
        }
    }
}

@Field static List ledNotificationEndpoints = [24, 25]
@Field static Map ledColorEndpoints = [103:18, 105:20]
@Field static Map ledIntensityEndpoints = [103:19, 105:21]
@Field static Map ledIntensityOffEndpoints = [104:22, 106:23]

@Field static Map configParams = [
    parameter001 : [
        number: 1,
        name:"Dimming Speed",
        description: "This changes the speed in which the attached light dims up or down. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed.",
        range: "0..100",
        default: 4,
        size: 1,
        type: "number",
        value: null
        ],
    parameter002 : [
        number: 2,
        name:"Dimming Speed (From Switch)",
        description: "This changes the speed in which the attached light dims up or down when controlled from the physical switch. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed. A setting of 99 should keep this in sync with parameter 1.",
        range: "0..99",
        default: 99,
        size: 1,
        type: "number",
        value: null
        ],
    parameter003 : [
        number: 3,
        name:"Ramp Rate",
        description: "This changes the speed in which the attached light turns on or off. For example, when a user sends the switch a basicSet(value: 0xFF) or basicSet(value: 0x00), this is the speed in which those actions take place. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed. A setting of 99 should keep this in sync with parameter 1.",
        range: "0..99",
        default: 99,
        size: 1,
        type: "number",
        value: null
        ],
    parameter004 : [
        number: 4,
        name:"Ramp Rate (From Switch)",
        description: "This changes the speed in which the attached light turns on or off from the physical switch. For example, when a user presses the up or down button, this is the speed in which those actions take place. A setting of 0 should turn the light immediately on or off (almost like an on/off switch). Increasing the value should slow down the transition speed. A setting of 99 should keep this in sync with parameter 1.",
        range: "0..99",
        default: 99,
        size: 1,
        type: "number",
        value: null
        ],
    parameter005 : [
        number: 5,
        name:"Minimum Light Level",
        description: "The minimum level that the dimmer allows the bulb to be dimmed to. Useful when the user has a bulb that does not turn on at a lower level.",
        range: "1..45",
        default: 1,
        size: 1,
        type: "number",
        value: null
        ],
    parameter006 : [
        number: 6,
        name:"Maximum Light Level",
        description: "The maximum level that the dimmer allows the bulb to be dimmed to. Useful when the user has an LED bulb that reaches its maximum level before the dimmer value of 99.",
        range: "55..99",
        default: 99,
        size: 1,
        type: "number",
        value: null
        ],
    parameter007 : [
        number: 7,
        name:"Minimum Fan Level",
        description: "The minimum level that the dimmer allows the fan to be dimmed to. Useful when the user has a fan that does not turn at a lower level.",
        range: "1..45",
        default: 1,
        size: 1,
        type: "number",
        value: null
        ],
    parameter008 : [
        number: 8,
        name:"Maximum Fan Level",
        description: "The maximum level that the dimmer allows the fan to be dimmed to.",
        range: "55..99",
        default: 99,
        size: 1,
        type: "number",
        value: null
        ],
    parameter010 : [
        number: 10,
        name:"Auto Off Light Timer",
        description: "Automatically turns the light switch off after this many seconds. When the switch is turned on a timer is started that is the duration of this setting. When the timer expires, the switch is turned off.",
        range: "0..32767",
        default: 0,
        size: 2,
        type: "number",
        value: null
        ],
    parameter011 : [
        number: 11,
        name:"Auto Off Fan Timer",
        description: "Automatically turns the fan switch off after this many seconds. When the switch is turned on a timer is started that is the duration of this setting. When the timer expires, the switch is turned off.",
        range: "0..32767",
        default: 0,
        size: 2,
        type: "number",
        value: null
        ],
    parameter012 : [
        number: 12,
        name:"Default Light Level (Local)",
        description: "Default level for the dimmer when it is powered on from the local switch. A setting of 0 means that the switch will return to the level that it was on before it was turned off.",
        range: "0..99",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter013 : [
        number: 13,
        name:"Default Light Level (Z-Wave)",
        description: "Default level for the dimmer when it is powered on from a Z-Wave command. A setting of 0 means that the switch will return to the level that it was on before it was turned off.",
        range: "0..99",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter014 : [
        number: 14,
        name:"Default Fan Level (Local)",
        description: "Default level for the dimmer when it is powered on from the local switch. A setting of 0 means that the switch will return to the level that it was on before it was turned off.",
        range: "0..99",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter015 : [
        number: 15,
        name:"Default Fan Level (Z-Wave)",
        description: "Default level for the dimmer when it is powered on from a Z-Wave command. A setting of 0 means that the switch will return to the level that it was on before it was turned off.",
        range: "0..99",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter016 : [
        number: 16,
        name:"Light State After Power Restored",
        description: "The state the switch should return to once power is restored after power failure. 0 = off, 1-99 = level, 100 = previous.",
        range: "0..100",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter017 : [
        number: 17,
        name:"Fan State After Power Restored",
        description: "The state the switch should return to once power is restored after power failure. 0 = off, 1-99 = level, 100 = previous.",
        range: "0..100",
        default: 0,
        size: 1,
        type: "number",
        value: null
        ],
    parameter018 : [
        number: 18,
        name:"Light LED Indicator Color",
        description: "This is the color of the LED strip for the Light.",
        range: ["0":"Red", "21":"Orange", "42":"Yellow", "85":"Green", "127":"Cyan", "170":"Blue", "212":"Violet", "234":"Pink", "255":"White"],
        default: 170,
        size: 2,
        type: "enum",
        value: null
        ],
    parameter019 : [
        number: 19,
        name:"Light LED Strip Intensity",
        description: "This is the intensity of the Light LED strip.",
        range: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"],
        default: 5,
        size: 1,
        type: "enum",
        value: null
        ],
    parameter020 : [
        number: 20,
        name:"Fan LED Indicator Color",
        description: "This is the color of the LED strip for the Fan.",
        range: ["0":"Red", "21":"Orange", "42":"Yellow", "85":"Green", "127":"Cyan", "170":"Blue", "212":"Violet", "234":"Pink", "255":"White"],
        default: 170,
        size: 2,
        type: "enum",
        value: null
        ],
    parameter021 : [
        number: 21,
        name:"Fan LED Strip Intensity",
        description: "This is the intensity of the Fan LED strip.",
        range: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"],
        default: 5,
        size: 1,
        type: "enum",
        value: null
        ],
    parameter022 : [
        number: 22,
        name:"Light LED Strip Intensity (When OFF)",
        description: "This is the intensity of the Light LED strip when the switch is off. This is useful for users to see the light switch location when the lights are off.",
        range: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"],
        default: 1,
        size: 1,
        type: "enum",
        value: null
        ],
    parameter023 : [
        number: 23,
        name:"Fan LED Strip Intensity (When OFF)",
        description: "This is the intensity of the Fan LED strip when the switch is off. This is useful for users to see the light switch location when the lights are off.",
        range: ["0":"0%", "1":"10%", "2":"20%", "3":"30%", "4":"40%", "5":"50%", "6":"60%", "7":"70%", "8":"80%", "9":"90%", "10":"100%"],
        default: 1,
        size: 1,
        type: "enum",
        value: null
        ],
    parameter026 : [
        number: 26,
        name:"Light LED Strip Timeout",
        description: "When the LED strip is disabled (Light LED Strip Intensity is set to 0), this setting allows the LED strip to turn on temporarily while being adjusted.",
        range: ["0":"Stay Off", "1":"1 Second", "2":"2 Seconds", "3":"3 Seconds", "4":"4 Seconds", "5":"5 Seconds", "6":"6 Seconds", "7":"7 Seconds", "8":"8 Seconds", "9":"9 Seconds", "10":"10 Seconds"],
        default: 3,
        size: 1,
        type: "enum",
        value: null
        ],
    parameter027 : [
        number: 27,
        name:"Fan LED Strip Timeout",
        description: "When the LED strip is disabled (Fan LED Strip Intensity is set to 0), this setting allows the LED strip to turn on temporarily while being adjusted.",
        range: ["0":"Stay Off", "1":"1 Second", "2":"2 Seconds", "3":"3 Seconds", "4":"4 Seconds", "5":"5 Seconds", "6":"6 Seconds", "7":"7 Seconds", "8":"8 Seconds", "9":"9 Seconds", "10":"10 Seconds"],
        default: 3,
        size: 1,
        type: "enum",
        value: null
        ],
    parameter028 : [
        number: 28,
        name:"Active Power Reports",
        description: "The power level change that will result in a new power report being sent. The value is a percentage of the previous report. 0 : disabled.",
        range: "0..100",
        default: 10,
        size: 1,
        type: "number",
        value: null
        ],
    parameter029 : [
        number: 29,
        name:"Periodic Power & Energy Reports",
        description: "Time period between consecutive power & energy reports being sent (in seconds). The timer is reset after each report is sent.",
        range: "0..32767",
        default: 3600,
        size: 2,
        type: "number",
        value: null
        ],
    parameter030 : [
        number: 30,
        name:"Energy Reports",
        description: "The energy level change that will result in a new energy report being sent. The value is a percentage of the previous report.",
        range: "0..100",
        default: 10,
        size: 1,
        type: "number",
        value: null
        ],
    parameter031 : [
        number: 31,
        name:"Local Protection Settings",
        description: "Enable local protection on these buttons.",
        range: ["0":"None", "1":"Light", "2":"Fan", "3":"Both"],
        default: 0,
        size: 1,
        type: "enum",
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
	[
     0x20: 1, // Basic
     0x70: 1, // Configuration
     0x98: 1, // Security
     0x60: 3, // Multi Channel
     0x8E: 2, // Multi Channel Association
     0x26: 3, // Switch Multilevel
     0x5B: 1, // Central Scene
     0x32: 3, // Meter
     0x85: 2, // Association
     0x86: 1, // Version
     0x75: 2  // Protection
    ]
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
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
    
    //New SmartThings app is changing child device to a "placeholder" device type
        if (useVDS != "true") checkChildTypes()
    
    def now
    if(location.timeZone)
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    else
    now = new Date().format("yyyy MMM dd EEE h:mm:ss a")
    sendEvent(name: "lastActivity", value: now, displayed:false)
    result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, ep = null) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd} - ep${ep?ep:0}"
    if (infoEnable != "false") log.info "${device.displayName}: Basic report received with value of ${cmd.value ? "on" : "off"} - ep${ep}"
    if (ep) {
        sendEvent(name: "switch${ep?ep:""}", value: cmd.value ? "on" : "off") 
        sendEvent(name: "level${ep?ep:""}", value: cmd.value == 99 ? 100 : cmd.value)
        if (cmd.value) {
            sendEvent([name: "switch", value: "on"])
        } else {
            def allOff = true
            if (device.currentState("switch${ep==1?2:1}")?.value != "off" || cmd.value ) allOff = false
        
            if (allOff) {
                sendEvent(name: "switch", value: "off")
            } else {
                sendEvent(name: "switch", value: "on")
            }
        }
        def childDevice = childDevices.find {
            it.deviceNetworkId == "$device.deviceNetworkId-ep00$ep"
        }
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: cmd.value ? "on" : "off")
            if (cmd.value && cmd.value <= 100) {
        	    childDevice.sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value)
            }
        }
        if (cmd.value) {
            sendEvent([name: "switch", value: "on"])
        } else {
            def allOff = true
            def children = childDevices
            childDevice = children.find{it.deviceNetworkId.endsWith("ep001")}
            if (childDevice?.currentState("switch")?.value != "off") allOff = false
            childDevice = children.find{it.deviceNetworkId.endsWith("ep002")}
            if (childDevice?.currentState("switch")?.value != "off") allOff = false

            if (allOff) {
                sendEvent([name: "switch", value: "off"])
            } else {
                sendEvent([name: "switch", value: "on"])
            }
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd) {
	if (debugEnable != "false") log.debug "${device.displayName}: ${cmd}"
    if (cmd.groupingIdentifier == 1) {
        if ([0,zwaveHubNodeId,0] == cmd.nodeId) state."associationMC${cmd.groupingIdentifier}" = true
        else state."associationMC${cmd.groupingIdentifier}" = false
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, ep = null) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd} - ep${ep?ep:0}"
    if (infoEnable != "false") log.info "${device.displayName}: Switch MultiLevel report received with value of ${cmd.value ? "on" : "off"} - ep${ep}"
        if (ep) {
        sendEvent(name: "switch${ep?ep:""}", value: cmd.value ? "on" : "off") 
        sendEvent(name: "level${ep?ep:""}", value: cmd.value == 99 ? 100 : cmd.value)
        if (cmd.value) {
            sendEvent([name: "switch", value: "on"])
        } else {
            def allOff = true
            if (device.currentState("switch${ep==1?2:1}")?.value != "off" || cmd.value ) allOff = false
        
            if (allOff) {
                sendEvent(name: "switch", value: "off")
            } else {
                sendEvent(name: "switch", value: "on")
            }
        }
        def childDevice = childDevices.find {
            it.deviceNetworkId == "$device.deviceNetworkId-ep00$ep"
        }
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: cmd.value ? "on" : "off")
            if (cmd.value && cmd.value <= 100) {
        	    childDevice.sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value)
            }
        }
        if (cmd.value) {
            sendEvent([name: "switch", value: "on"])
        } else {
            def allOff = true
            def children = childDevices
            childDevice = children.find{it.deviceNetworkId.endsWith("ep001")}
            if (childDevice?.currentState("switch")?.value != "off") allOff = false
            childDevice = children.find{it.deviceNetworkId.endsWith("ep002")}
            if (childDevice?.currentState("switch")?.value != "off") allOff = false

            if (allOff) {
                sendEvent([name: "switch", value: "off"])
            } else {
                sendEvent([name: "switch", value: "on"])
            }
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd}"
    def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd}"
    if(cmd.applicationVersion != null && cmd.applicationSubVersion != null) {
	    def firmware = "${cmd.applicationVersion}.${cmd.applicationSubVersion.toString().padLeft(2,'0')}"
        if (infoEnable != "false") log.info "${device.displayName}: Firmware report received: ${firmware}"
        state.needfwUpdate = "false"
        createEvent(name: "firmware", value: "${firmware}")
    } else if(cmd.firmware0Version != null && cmd.firmware0SubVersion != null) {
	    def firmware = "${cmd.firmware0Version}.${cmd.firmware0SubVersion.toString().padLeft(2,'0')}"
        if (infoEnable != false) log.info "${device.displayName}: Firmware report received: ${firmware}"
        state.needfwUpdate = "false"
        createEvent(name: "firmware", value: "${firmware}")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd}"
    if (infoEnable != "false") log.info "${device.displayName}: parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd.scaledConfigurationValue}'"
    def integerValue = cmd.scaledConfigurationValue.toInteger()
    if (!state.lastRan || now() <= state.lastRan + 60000) {
        state."parameter${cmd.parameterNumber}value" = integerValue
    } else {
        if (infoEnable == "true") log.info "${device.label?device.label:device.name}: Configuration report received more than 60 seconds after running updated(). Possible configuration made at switch"
    }
    switch (cmd.parameterNumber) {
        case 12:
        case 13:
            def childDevice = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-ep1${cmd.parameterNumber}"}
            if(childDevice) { 
                childDevice.sendEvent(name:"switch", value:cmd.scaledConfigurationValue? "on":"off")
                childDevice.sendEvent(name:"level", value:cmd.scaledConfigurationValue)
            }
        break
        case 18:
        case 20:
            def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep${ledColorEndpoints.find { it.value == cmd.parameterNumber }?.key}")}
            if (childDevice) {
                if (integerValue == 255) {
                    childDevice.sendEvent(name: "colorTemperature", value: state.colorTemperature?state.colorTemperature:0)
                    childDevice.sendEvent(name: "colorMode", value: "CT", descriptionText: "${device.getDisplayName()} color mode is CT")
                } else {
                    childDevice.sendEvent(name:"hue", value:"${Math.round(zwaveValueToHuePercent(integerValue))}")
                    childDevice.sendEvent(name:"saturation", value:"100")
                    childDevice.sendEvent(name: "colorMode", value: "RGB", descriptionText: "${device.getDisplayName()} color mode is RGB")
                }
            }
        break
        case 19:
        case 21:
            def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep${ledIntensityEndpoints.find { it.value == cmd.parameterNumber }?.key}")}
            if (childDevice) {
                childDevice.sendEvent(name:"level", value:"${integerValue*10}")
                childDevice.sendEvent(name:"switch", value:"${integerValue==0?"off":"on"}")
            }
        break
        case 22:
        case 23:
            def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep${ledIntensityOffEndpoints.find { it.value == cmd.parameterNumber }?.key}")}
            if (childDevice) {
                childDevice.sendEvent(name:"level", value:"${integerValue*10}")
                childDevice.sendEvent(name:"switch", value:"${integerValue==0?"off":"on"}")
            }
        break
        case 24:
            if (cmd.scaledConfigurationValue == 0) {
                toggleTiles(24, "off")
            }
        break
        case 25:
            if (cmd.scaledConfigurationValue == 0) {
                toggleTiles(25, "off")
            }
        break
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    if (debugEnable != "false") log.debug "${device.displayName} Unhandled Event: ${cmd}"
}

def on() {
    if (infoEnable != "false") log.info "${device.displayName}: on()"
    commands([
            zwave.basicV1.basicSet(value: 0xFF)
    ])
}

def off() {
    if (infoEnable != "false") log.info "${device.displayName}: off()"
    commands([
            zwave.basicV1.basicSet(value: 0x00)
    ])
}

def setLevel(value) {
    if (infoEnable != "false") log.info "${device.displayName}: setLevel($value)"
    commands([
        zwave.basicV1.basicSet(value: value < 100 ? value : 99)
    ])
}

private toggleTiles(number, value) {
   if ((number.toInteger() >= 201 && number.toInteger() <= 205) || number.toInteger() == 24){
   for (int i = 201; i <= 205; i++){
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
   if ((number.toInteger() >= 251 && number.toInteger() <= 255) || number.toInteger() == 25){
   for (int i = 251; i <= 255; i++){
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

def startNotification(value, ep = null){
    if (infoEnable != "false") log.info "${device.displayName}: startNotification($value)"
    def parameterNumbers = [24,25]
    def cmds = []
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value.toInteger(), parameterNumber: parameterNumbers[(ep == null)? 0:ep?.toInteger()-1], size: 4)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: parameterNumbers[(ep == null)? 0:ep?.toInteger()-1])
    return commands(cmds)
}

def stopNotification(ep = null){
    if (infoEnable != "false") log.info "${device.displayName}: stopNotification()"
    def parameterNumbers = [24,25]
    def cmds = []
    cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value.toInteger(), parameterNumber: parameterNumbers[(ep == null)? 0:ep?.toInteger()-1], size: 4)
    cmds << zwave.configurationV1.configurationGet(parameterNumber: parameterNumbers[(ep == null)? 0:ep?.toInteger()-1])
    return commands(cmds)
}

def on1() {
    if (infoEnable != "false") log.info "${device.displayName}: on1()"
    return command(encap(zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF), 1))
}

def on2() {
    if (infoEnable != "false") log.info "${device.displayName}: on2()"
    return command(encap(zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF), 2))
}

def off1() {
    if (infoEnable != "false") log.info "${device.displayName}: off1()"
    return command(encap(zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00), 1))
}

def off2() {
    if (infoEnable != "false") log.info "${device.displayName}: off1()"
    return command(encap(zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00), 2))
}

def setLevel1(value, duration = null) {
    if (infoEnable != "false") log.info "${device.displayName}: setLevel1($value, $duration)"
    def valueaux = value as Integer
    def level = Math.max(Math.min(valueaux, 99), 0) 
    if (duration == null) {
        return command(encap(zwave.switchMultilevelV2.switchMultilevelSet(value: level), 1))
    } else {
        def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
        return command(encap(zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration), 1))
    }
}

def setLevel2(value, duration = null) {
    if (infoEnable != "false") log.info "${device.displayName}: setLevel2($value, $duration)"
    def valueaux = value as Integer
    def level = Math.max(Math.min(valueaux, 99), 0)  
    if (duration == null) {
        return command(encap(zwave.switchMultilevelV2.switchMultilevelSet(value: level), 2))
    } else {
        def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
        return command(encap(zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration), 2))
    }
}

void childSetLevel(String dni, value) {
    if (infoEnable != "false") log.info "${device.displayName}: childSetLevel($dni, $value)"
    state.lastRan = now()
    def valueaux = value as Integer
    def level = Math.max(Math.min(valueaux, 99), 0)    
    def cmds = []
    switch (channelNumber(dni).toInteger()) {
        case 103:
        case 105:
            cmds << setParameter(ledIntensityEndpoints[channelNumber(dni)], Math.round(level/10), 1)
            cmds << getParameter(ledIntensityEndpoints[channelNumber(dni)])
        break
        case 104:
        case 106:
            cmds << setParameter(ledIntensityOffEndpoints[channelNumber(dni)], Math.round(level/10), 1)
            cmds << getParameter(ledIntensityOffEndpoints[channelNumber(dni)])
        break
        case 112:
            cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 12, size: 1) 
            cmds << zwave.configurationV1.configurationGet(parameterNumber: 12 )
        break
        case 113:
            cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 13, size: 1) 
            cmds << zwave.configurationV1.configurationGet(parameterNumber: 13 )
        break
        case 151:
            cmds << zwave.protectionV2.protectionSet(localProtectionState : level > 0 ? 1 : 0, rfProtectionState: state.rfProtectionState? state.rfProtectionState:0) 
            cmds << zwave.protectionV2.protectionGet() 
        break
        case 152:
            cmds << zwave.protectionV2.protectionSet(localProtectionState : state.localProtectionState? state.localProtectionState:0, rfProtectionState : level > 0 ? 1 : 0) 
            cmds << zwave.protectionV2.protectionGet() 
        break
        case 1:
        case 2:
            cmds << encap(zwave.switchMultilevelV1.switchMultilevelSet(value: level), channelNumber(dni).toInteger())
            //cmds << encap(zwave.switchMultilevelV1.switchMultilevelGet(), channelNumber(dni))
        break
    }
	if(cmds) sendHubCommand(commands(cmds))
}

void childOn(String dni) {
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
            cmds << setParameter(24, calculateParameter("24-${channelNumber(dni)}"), 4)
        break
        case 251:
        case 252:
        case 253:
        case 254:
        case 255:
            toggleTiles("${channelNumber(dni)}", "on")
            cmds << setParameter(25, calculateParameter("25-${channelNumber(dni)}"), 4)
        break
        case 112:
        case 113:
        case 151:
        case 152:
            childSetLevel(dni, 99)
        break
        case 1:
        case 2:
            cmds << encap(zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF), channelNumber(dni).toInteger())
        break
    }
    if(cmds) sendHubCommand(commands(cmds))
}

void childOff(String dni) {
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
            cmds << setParameter(24, 0, 4)
        break
        case 251:
        case 252:
        case 253:
        case 254:
        case 255:
            toggleTiles("${channelNumber(dni)}", "off")
            cmds << setParameter(25, 0, 4)
        break
        case 112:
        case 113:
        case 151:
        case 152:
            childSetLevel(dni, 0)
        break
        case 1:
        case 2:
            cmds << encap(zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00), channelNumber(dni).toInteger())
        break
    }
    if(cmds) sendHubCommand(commands(cmds))
}

void childRefresh(String dni) {
    if (infoEnable != "false") log.info "${device.displayName}: childRefresh($dni)"
    def cmds = []
    cmds << encap(zwave.switchMultilevelV1.switchMultilevelGet(value: 0x00), channelNumber(dni).toInteger())
    if(cmds) sendHubCommand(commands(cmds))
}

void componentSetColor(dni, value) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: dni, componentSetColor($value)"
	if (value.hue == null || value.saturation == null) return
	def ledColor = Math.round(huePercentToZwaveValue(value.hue))
    state.lastRan = now()
	if (infoEnable) log.info "${device.label?device.label:device.name}: Setting LED color value to $ledColor & LED intensity to $ledLevel"
    def cmds = []
    if (value.level != null) {
        def ledLevel = Math.round(value.level/10)
        cmds << setParameter(ledIntensityEndpoints[channelNumber(dni)], ledLevel, 1)
        cmds << getParameter(ledIntensityEndpoints[channelNumber(dni)])
    }
    cmds << setParameter(ledColorEndpoints[channelNumber(dni)], ledColor, 2)
    cmds << getParameter(ledColorEndpoints[channelNumber(dni)])
    if(cmds) sendHubCommand(commands(cmds))
}

void componentSetColorTemperature(dni, value) {
    if (infoEnable != "false") log.info "${device.label?device.label:device.name}: dni, componentSetColorTemperature($value)"
    if (infoEnable) log.info "${device.label?device.label:device.name}: Setting LED color value to 255"
    state.lastRan = now()
    state.colorTemperature = value
    def cmds = []
    cmds << setParameter(ledColorEndpoints[channelNumber(dni)], 255, 2)
    cmds << getParameter(ledColorEndpoints[channelNumber(dni)])
    if(cmds) sendHubCommand(commands(cmds))
}

def setSpeed(value){
    if (infoEnable) log.info "${device.label?device.label:device.name}: setSpeed($value)"
    def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep002")}
    if(childDevice) childDevice.sendEvent(name: "speed", value: value)
    switch (value) {
        case "low":
            return childSetLevel("${device.deviceNetworkId}-ep002",33)
        break
        case "medium-low":
        case "medium":
        case "medium-high":
            return childSetLevel("${device.deviceNetworkId}-ep002",66)
        break
        case "high":
            return childSetLevel("${device.deviceNetworkId}-ep002",99)
        break
        case "auto":
            return childSetLevel("${device.deviceNetworkId}-ep002",1)
        break
        case "on":
            return childOn("${device.deviceNetworkId}-ep002")
        break
        case "off":
            return childOff("${device.deviceNetworkId}-ep002")
        break
    }
}

def cycleSpeed() {
    def currentSpeed = "off"
    def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep002")}
    if (childDevice) currentSpeed = childDevice.currentValue("speed")? childDevice.currentValue("speed") : "off"
    switch (currentSpeed) {
        case "off":
            return childSetLevel("${device.deviceNetworkId}-ep002",33)
        break
        case "low":
            return childSetLevel("${device.deviceNetworkId}-ep002",66)
        break
        case "medium-low":
        case "medium":
        case "medium-high":
            return childSetLevel("${device.deviceNetworkId}-ep002",99)
        break
        case "high":
            return childOff("${device.deviceNetworkId}-ep002")
        break
    }
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

def configure() {
    if (infoEnable != "false") log.info "${device.displayName}: configure()"
    def cmds = initialize()
    commands(cmds)
}

def refresh() {
    if (infoEnable != "false") log.info "refresh()"
    def cmds = []
    cmds << encap(zwave.switchMultilevelV1.switchMultilevelGet(), 1)
    cmds << encap(zwave.switchMultilevelV1.switchMultilevelGet(), 2)
    cmds << zwave.meterV3.meterGet(scale: 0)
    cmds << zwave.meterV3.meterGet(scale: 2)
    return commands(cmds, 100)
}

def reset() {
    if (infoEnable != "false") log.info "${device.displayName}: Resetting energy statistics"
    def cmds = []
    cmds << zwave.meterV2.meterReset()
    cmds << zwave.meterV2.meterGet(scale: 0)
    cmds << zwave.meterV2.meterGet(scale: 2)
    commands(cmds, 1000)
}

def installed() {
    if (infoEnable != "false") log.info "installed()"
}

def updated() {
    if (!state.lastRan || now() >= state.lastRan + 2000) {
        if (infoEnable != "false") log.info "${device.displayName}: updated()"
        if (debugEnable && disableDebugLogging) runIn(disableDebugLogging*60,debugLogsOff)
        if (infoEnable && disableInfoLogging) runIn(disableInfoLogging*60,infoLogsOff)
        state.lastRan = now()
        def cmds = initialize()
        if (cmds != [])
            response(commands(cmds, 1000))
        else 
            return null
    } else {
        if (infoEnable != "false") log.info "${device.displayName}: updated() ran within the last 2 seconds. Skipping execution."
    }
}

private checkChildTypes() {
    def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep001")}
    if (childDevice && (childDevice.typeName.indexOf("Switch Level Child Device") < 0)) {
        if (infoEnable != "false") log.info "${device.displayName}: The new SmartThings app is changing child device to the incorrect device handler. Changing it back."
        childDevice.setDeviceType("InovelliUSA","Switch Level Child Device")
    }
    childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep002")}
    if (childDevice && (childDevice.typeName.indexOf("Switch Level Child Device") < 0) && (childDevice.typeName.indexOf("Fan Child Device") < 0)) {
        if (infoEnable != "false") log.info "${device.displayName}: The new SmartThings app is changing child device to the incorrect device handler. Changing it back."
        childDevice.setDeviceType("InovelliUSA","Switch Level Child Device")
    }
}

def debugLogsOff(){
    //log.warn "${device.label?device.label:device.name}: Disabling debug logging after timeout"
    device.updateSetting("debugEnable", [type: "boolean", value: "false"])
}

def infoLogsOff(){
    log.warn "${device.label?device.label:device.name}: Disabling info logging after timeout"
    device.updateSetting("infoEnable", [type: "boolean", value: "false"])
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
    sendEvent(name: "checkInterval", value: 3 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    sendEvent(name: "numberOfButtons", value: 9, displayed: false)
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["pushed","held","pushed_2x","pushed_3x","pushed_4x","pushed_5x"]), displayed:false)
    
    if (useVDS != "true") {
        addChild("ep001", "Light", "InovelliUSA", "Switch Level Child Device", false)
        addChild("ep002", "Fan", "InovelliUSA", "Switch Level Child Device", false)
    }

    if (enableDefaultLocalChild == "true") addChild("ep112", "Default Local Level", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep112")
    if (enableDefaultZWaveChild == "true") addChild("ep113", "Default Z-Wave Level", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep113")
    if (enableDisableLocalChild == "true") addChild("ep151", "Disable Local Control", "smartthings", "Child Switch", false)
    else deleteChild("ep151")
    if (enableDisableRemoteChild == "true") addChild("ep152", "Disable Remote Control", "smartthings", "Child Switch", false)
    else deleteChild("ep152")
    if (enableLED1Child == "true") addChild("ep103", "Light LED", "InovelliUSA", "RGBW Child Device", false)
    else deleteChild("ep103")
    if (enableLED1OffChild == "true") addChild("ep104", "Light LED - When Off", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep104")
    if (enableLED2Child == "true") addChild("ep105", "Fan LED", "InovelliUSA", "RGBW Child Device", false)
    else deleteChild("ep105")
    if (enableLED2OffChild == "true") addChild("ep106", "Fan LED - When Off", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep106")
    
    [201,202,203,204,205].each { i ->
        if ((settings."parameter24-${i}a"!=null && settings."parameter24-${i}b"!=null && settings."parameter24-${i}c"!=null && settings."parameter24-${i}d"!=null && settings."parameter24-${i}d"!="0") && !childExists("ep${i}")) {
            addChild("ep${i}", "Light Notification ${i-200}", "smartthings", "Child Switch", false)
        } else if ((settings."parameter24-${i}a"==null || settings."parameter24-${i}b"==null || settings."parameter24-${i}c"==null || settings."parameter24-${i}d"==null || settings."parameter24-${i}d"=="0") && childExists("ep${i}")) {
            deleteChild("ep${i}")
        }
    }
    [251,252,253,254,255].each { i ->
        if ((settings."parameter25-${i}a"!=null && settings."parameter25-${i}b"!=null && settings."parameter25-${i}c"!=null && settings."parameter25-${i}d"!=null && settings."parameter25-${i}d"!="0") && !childExists("ep${i}")) {
            addChild("ep${i}", "Fan Notification ${i-250}", "smartthings", "Child Switch", false)
        } else if ((settings."parameter25-${i}a"==null || settings."parameter25-${i}b"==null || settings."parameter25-${i}c"==null || settings."parameter25-${i}d"==null || settings."parameter25-${i}d"=="0") && childExists("ep${i}")) {
            deleteChild("ep${i}")
        }
    }
    
    if (enableDefaultLocalChild == "true") addChild("ep112", "Default Local Level", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep112")
    if (enableDefaultZWaveChild == "true") addChild("ep113", "Default Z-Wave Level", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep113")
    if (enableDisableLocalChild == "true") addChild("ep151", "Disable Local Control", "smartthings", "Child Switch", false)
    else deleteChild("ep151")
    if (enableDisableRemoteChild == "true") addChild("ep152", "Disable Remote Control", "smartthings", "Child Switch", false)
    else deleteChild("ep152")
    if (enableLED1Child == "true") addChild("ep103", "Light LED", "InovelliUSA", "RGBW Child", false)
    else deleteChild("ep103")
    if (enableLED1OffChild == "true") addChild("ep104", "Light LED - When Off", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep104")
    if (enableLED2Child == "true") addChild("ep105", "Fan LED", "InovelliUSA", "RGBW Child", false)
    else deleteChild("ep105")
    if (enableLED2OffChild == "true") addChild("ep106", "Fan LED - When Off", "InovelliUSA", "Switch Level Child Device", false)
    else deleteChild("ep106")
    
    if (device.displayName != state.oldLabel) {
        def childDevice = childDevices.find{it.deviceNetworkId.endsWith("ep101")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Disable Local Control)")
        childDevice.setLabel("${device.displayName} (Disable Local Control)")
        childDevice = children.find{it.deviceNetworkId.endsWith("ep102")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Disable Remote Control)")
        childDevice.setLabel("${device.displayName} (Disable Remote Control)")
        
        childDevice = children.find{it.deviceNetworkId.endsWith("ep103")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Light LED)")
        childDevice.setLabel("${device.displayName} (Light LED)")
        childDevice = children.find{it.deviceNetworkId.endsWith("ep104")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Light LED - When Off)")
        childDevice.setLabel("${device.displayName} (Light LED - When Off)")
        
        childDevice = children.find{it.deviceNetworkId.endsWith("ep105")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Fan LED)")
        childDevice.setLabel("${device.displayName} (Fan LED)")
        childDevice = children.find{it.deviceNetworkId.endsWith("ep106")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Fan LED - When Off)")
        childDevice.setLabel("${device.displayName} (Fan LED - When Off)")
        
        childDevice = children.find{it.deviceNetworkId.endsWith("ep001")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Light)")
        childDevice.setLabel("${device.displayName} (Light)")
        childDevice = children.find{it.deviceNetworkId.endsWith("ep002")}
        if (childDevice && childDevice.displayName == "${state.oldLabel} (Fan)")
        childDevice.setLabel("${device.displayName} (Fan)")
    }
    state.oldLabel = device.displayName
        
    /*
    sendEvent([name:"pressUpX1", value:pressUpX1Label? "${pressUpX1Label} ▲" : "Tap ▲", displayed: false])
    sendEvent([name:"pressDownX1", value:pressDownX1Label? "${pressDownX1Label} ▼" : "Tap ▼", displayed: false])
    sendEvent([name:"pressUpX2", value:pressUpX2Label? "${pressUpX2Label} ▲▲" : "Tap ▲▲", displayed: false])
    sendEvent([name:"pressDownX2", value:pressDownX2Label? "${pressDownX2Label} ▼▼" : "Tap ▼▼", displayed: false])
    sendEvent([name:"pressUpX3", value:pressUpX3Label? "${pressUpX3Label} ▲▲▲" : "Tap ▲▲▲", displayed: false])
    sendEvent([name:"pressDownX3", value:pressDownX3Label? "${pressDownX3Label} ▼▼▼" : "Tap ▼▼▼", displayed: false])
    sendEvent([name:"pressUpX4", value:pressUpX4Label? "${pressUpX4Label} ▲▲▲▲" : "Tap ▲▲▲▲", displayed: false])
    sendEvent([name:"pressDownX4", value:pressDownX4Label? "${pressDownX4Label} ▼▼▼▼" : "Tap ▼▼▼▼", displayed: false])
    sendEvent([name:"pressUpX5", value:pressUpX5Label? "${pressUpX5Label} ▲▲▲▲▲" : "Tap ▲▲▲▲▲", displayed: false])
    sendEvent([name:"pressDownX5", value:pressDownX5Label? "${pressDownX5Label} ▼▼▼▼▼" : "Tap ▼▼▼▼▼", displayed: false])
    sendEvent([name:"holdUp", value:pressHoldUpLabel? "${pressHoldUpLabel} ▲" : "Hold ▲", displayed: false])
    sendEvent([name:"holdDown", value:pressHoldDownLabel? "${pressHoldDownLabel} ▼" : "Hold ▼", displayed: false])
    */
    
    def cmds = processAssociations()
    
    if (group2e1 != "" && group2e1 != null) {
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 2, nodeId: [0,Integer.parseInt(group2e1,16),1])
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 2)
    }
    if (group4e1 != "" && group4e1 != null) {
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 4, nodeId: [0,Integer.parseInt(group4e1,16),1])
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 4)
    }
    if (group5e1 != "" && group5e1 != null) {
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 5, nodeId: [0,Integer.parseInt(group5e1,16),2])
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 5)
    }
    if (group7e1 != "" && group7e1 != null) {
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 7, nodeId: [0,Integer.parseInt(group7e1,16),2])
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 7)
    }
    
    if(!state.associationMC1) {
       log.debug "Adding MultiChannel association group 1"
       cmds << zwave.associationV2.associationRemove(groupingIdentifier: 1, nodeId: [])
       cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 1, nodeId: [0,zwaveHubNodeId,0])
       cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 1)
    }
    
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
def childExists(ep) {
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith(ep)}
    if (childDevice) 
        return true
    else
        return false
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd, ep=null) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd} - ep${ep?ep:0}"
    switch (cmd.keyAttributes) {
       case 0:
       if (cmd.sceneNumber == 3) createEvent(buttonEvent(7, "pushed", "physical"))
       else if (cmd.sceneNumber == 4) createEvent(buttonEvent(7, "held", "physical"))
       else if (cmd.sceneNumber == 5) createEvent(buttonEvent(9, "pushed", "physical"))
       else if (cmd.sceneNumber == 6) createEvent(buttonEvent(9, "held", "physical"))
       else createEvent(buttonEvent(cmd.keyAttributes + 1, (cmd.sceneNumber == 2? "pushed" : "held"), "physical"))
       break
       case 1:
       createEvent(buttonEvent(6, (cmd.sceneNumber == 2? "pushed" : "held"), "physical"))
       break
       case 2:
       createEvent(buttonEvent(8, (cmd.sceneNumber == 2? "pushed" : "held"), "physical"))
       break
       default:
       createEvent(buttonEvent(cmd.keyAttributes - 1, (cmd.sceneNumber == 2? "pushed" : "held"), "physical"))
       break
    }
}

def buttonEvent(button, value, type = "digital") {
    if(button != 6)
        sendEvent(name:"lastEvent", value: "${value != 'pushed'?' Tap '.padRight(button+5, '▼'):' Tap '.padRight(button+5, '▲')}", displayed:false)
    else
        sendEvent(name:"lastEvent", value: "${value != 'pushed'?' Hold ▼':' Hold ▲'}", displayed:false)
    if (infoEnable != "false") log.info "${device.displayName}: Button ${button} was ${value}"
    [name: "button", value: value, data: [buttonNumber: button], descriptionText: "$device.displayName button $button was $value", isStateChange: true, type: type]
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd, ep=null) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd} ${ep?ep:0}"
    def event
	if (cmd.scale == 0) {
    	if (cmd.meterType == 161) {
		    event = createEvent(name: "voltage", value: cmd.scaledMeterValue, unit: "V")
            if (infoEnable != "false") log.info "${device.displayName}: Voltage report received with value of ${cmd.scaledMeterValue} V"
        } else if (cmd.meterType == 1) {
        	event = createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
            if (infoEnable != "false") log.info "${device.displayName}: Energy report received with value of ${cmd.scaledMeterValue} kWh"
        }
	} else if (cmd.scale == 1) {
		event = createEvent(name: "amperage", value: cmd.scaledMeterValue, unit: "A")
        if (infoEnable != "false") log.info "${device.displayName}: Amperage report received with value of ${cmd.scaledMeterValue} A"
	} else if (cmd.scale == 2) {
		event = createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W")
        if (infoEnable != "false") log.info "${device.displayName}: Power report received with value of ${cmd.scaledMeterValue} W"
	}

    return event
}

def getParameterNumbers(){
    return [1,2,3,4,5,6,7,8,10,11,12,13,14,15,16,17,18,19,20,21,22,23,26,27,28,29,30,31,51]
}

private encap(cmd, endpoint) {
    if (endpoint) {
        zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint: endpoint).encapsulate(cmd)
    } else {
        cmd
    }
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

private channelNumber(String dni) {
    dni.split("-ep")[-1] as Integer
}

def calculateParameter(number) {
    def value = 0
    switch (number){
      case "13":
          if (settings.parameter13custom =~ /^([0-9]{1}|[0-9]{2}|[0-9]{3})$/) value = settings.parameter13custom.toInteger() / 360 * 255
          else value = settings."parameter${number}"
      break
      case "24-201":
      case "24-202":
      case "24-203": 
      case "24-204":
      case "24-205":
         value += settings."parameter${number}a"!=null ? settings."parameter${number}a".toInteger() * 1 : 0
         value += settings."parameter${number}b"!=null ? settings."parameter${number}b".toInteger() * 256 : 0
         value += settings."parameter${number}c"!=null ? settings."parameter${number}c".toInteger() * 65536 : 0
         value += settings."parameter${number}d"!=null ? settings."parameter${number}d".toInteger() * 16777216 : 0
      break
      case "25-251":
      case "25-252":
      case "25-253": 
      case "25-254":
      case "25-255":
         value += settings."parameter${number}a"!=null ? settings."parameter${number}a".toInteger() * 1 : 0
         value += settings."parameter${number}b"!=null ? settings."parameter${number}b".toInteger() * 256 : 0
         value += settings."parameter${number}c"!=null ? settings."parameter${number}c".toInteger() * 65536 : 0
         value += settings."parameter${number}d"!=null ? settings."parameter${number}d".toInteger() * 16777216 : 0
      break
      default:
          value = settings."parameter${number}"
      break
    }
    return value
}

def setParameter(number, value, size) {
    if (infoEnable != "false") log.info "${device.displayName}: Setting parameter $number with a size of $size bytes to $value"
    return zwave.configurationV1.configurationSet(scaledConfigurationValue: value.toInteger(), parameterNumber: number, size: size)
}

def getParameter(number) {
    //if (infoEnable != "false") log.info "${device.displayName}: Retreiving value of parameter $number"
    return zwave.configurationV1.configurationGet(parameterNumber: number)
}

def setDefaultAssociations() {
    def smartThingsHubID = (zwaveHubNodeId.toString().format( '%02x', zwaveHubNodeId )).toUpperCase()
    state.defaultG1 = []
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
   if (state.associationGroups) {
       associationGroups = state.associationGroups
   } else {
       if (infoEnable != "false") log.info "${device.displayName}: Getting supported association groups from device"
       cmds <<  zwave.associationV2.associationGroupingsGet()
   }
   for (int i = 1; i <= associationGroups; i++){
      if(state."actualAssociation${i}" != null){
         if(state."desiredAssociation${i}" != null || state."defaultG${i}") {
            def refreshGroup = false
            ((state."desiredAssociation${i}"? state."desiredAssociation${i}" : [] + state."defaultG${i}") - state."actualAssociation${i}").each {
                if (it != null){
                    if (infoEnable != "false") log.info "${device.displayName}: Adding node $it to group $i"
                    cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))
                    refreshGroup = true
                }
            }
            ((state."actualAssociation${i}" - state."defaultG${i}") - state."desiredAssociation${i}").each {
                if (it != null){
                    if (infoEnable != "false") log.info "${device.displayName}: Removing node $it from group $i"
                    cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))
                    refreshGroup = true
                }
            }
            if (refreshGroup == true) cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
            else if (infoEnable != "false") log.info "${device.displayName}: There are no association actions to complete for group $i"
         }
      } else {
         if (infoEnable != "false") log.info "${device.displayName}: Association info not known for group $i. Requesting info from device."
         cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
      }
   }
   return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd}"
    def temp = []
    if (cmd.nodeId != []) {
       cmd.nodeId.each {
          temp += it.toString().format( '%02x', it.toInteger() ).toUpperCase()
       }
    } 
    state."actualAssociation${cmd.groupingIdentifier}" = temp
    if (infoEnable != "false") log.info "${device.displayName}: Associations for Group ${cmd.groupingIdentifier}: ${temp}"
    updateDataValue("associationGroup${cmd.groupingIdentifier}", "$temp")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd}"
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    if (infoEnable != "false") log.info "${device.displayName}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}

def zwaveEvent(physicalgraph.zwave.commands.protectionv2.ProtectionReport cmd, ep=null) {
    if (debugEnable != "false") log.debug "${device.displayName}: ${cmd} - $ep"
    if (infoEnable != "false") log.info "${device.displayName}: Protection report received: Local protection is ${cmd.localProtectionState > 0 ? "on" : "off"} & Remote protection is ${cmd.rfProtectionState > 0 ? "on" : "off"}"
    state.localProtectionState = cmd.localProtectionState
    state.rfProtectionState = cmd.rfProtectionState
    //device.updateSetting("disableLocal",[value:cmd.localProtectionState?"1":"0",type:"enum"])
    //device.updateSetting("disableRemote",[value:cmd.rfProtectionState?"1":"0",type:"enum"])
    def children = childDevices
    def childDevice = children.find{it.deviceNetworkId.endsWith("ep151")}
    if (childDevice) {
        childDevice.sendEvent(name: "switch", value: cmd.localProtectionState > 0 ? "on" : "off")        
    }
    childDevice = children.find{it.deviceNetworkId.endsWith("ep152")}
    if (childDevice) {
        childDevice.sendEvent(name: "switch", value: cmd.rfProtectionState > 0 ? "on" : "off")        
    }
}
