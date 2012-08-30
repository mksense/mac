#include <Uberdust.h>

#include <XbeeRadio.h>
#include <XBee.h>
#include <stdio.h>
#include <string.h>
#include <SoftwareSerial.h>


// Create the xbee object
XBeeRadio xbee = XBeeRadio(); 

// Create reusable response objects for responses we expect to handle 
XBeeRadioResponse response = XBeeRadioResponse();  

// Allocate two bytes for to hold a 32-bit analog reading
uint8_t payload[] = { 
  102, 0,  0, 0, 0, 0};

// 16-bit addressing: Enter address of remote XBee, typically the coordinator
Tx16Request tx = Tx16Request(0xffff, payload, sizeof(payload));

uint8_t tmpPayload[20];
Tx16Request tmpTx = Tx16Request(0xffff, tmpPayload, sizeof(tmpPayload)); 

TxStatusResponse txStatus = TxStatusResponse();

uint8_t lampPins[] = { 2, 3, 4, 5, 6};
uint8_t relayCheckPin = A4;
uint8_t numOfRelays = 0;

uint8_t lampStatuses[5] = { 0, 0, 0, 0, 0};

uint8_t pirPin = 9;
uint8_t heaterPin = 10;
uint8_t securityPin = 11;
uint8_t sensorsCheckPin = 12;
uint8_t ledPin = 13;
uint8_t tempPin = A0;
uint8_t lightPin = A1;
uint8_t methanePin = A2;
uint8_t carbonPin = A3;
bool sensorsExist = false;

uint8_t pirStatus;
uint8_t securityStatus;
int tempValue=0;
int lightValue=0;
int methaneValue=0;
int carbonValue=0;


Uberdust uber = Uberdust();

void setup()
{

  xbee.initialize_xbee_module();
  // setup xbee 
  xbee.begin(38400);
  xbee.init(12);

  uber.setup(&xbee, &tx);

}

void loop()
{
  static unsigned long ledTimestamp = 0;
  if(millis() - ledTimestamp > 5000)
  {
    uber.sendValue("light1", 1);
    uber.sendValue("temp", 4553.4);
    uber.sendValue("hehehe", "bla");
    uber.sendValue("ha", String("bla")+3);
    uber.blinkLED(1,100);
    ledTimestamp = millis();
  }

}


