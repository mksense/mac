//This example reads a MIFARE memory block. It is tested with a new MIFARE 1K cards. Uses default keys.
//Contributed by Seeed Technology Inc (www.seeedstudio.com)

#include <PN532.h>

#define SCK 13
#define MOSI 11
#define SS 10
#define MISO 12

PN532 nfc(SCK, MISO, MOSI, SS);




#include <XbeeRadio.h>
#include <XBee.h>

#include <Uberdust.h>

// Create the xbee object
XBeeRadio xbee = XBeeRadio(); 

// Create reusable response objects for responses we expect to handle 
XBeeRadioResponse response = XBeeRadioResponse();  

// Allocate two bytes for to hold a 32-bit analog reading
uint8_t payload[] = { 
  102, 0,  0, 0, 0, 0};

// 16-bit addressing: Enter address of remote XBee, typically the coordinator
Tx16Request tx = Tx16Request(0xffff, payload, sizeof(payload));

TxStatusResponse txStatus = TxStatusResponse();

Uberdust uber = Uberdust();

void setup(void)
{
  xbee.initialize_xbee_module();
  // setup xbee 
  xbee.begin(38400);
  xbee.init();

  uber.setup(&xbee, &tx);
  
  
  
  nfc.begin();
  uint32_t versiondata = nfc.getFirmwareVersion();
  if (! versiondata)
    while (1); // halt

  // configure board to read RFID tags and cards
  nfc.SAMConfig();
}


void loop(void)
{
  checkRFID();
  periodicCapabilities();
}


void periodicCapabilities()
{
  static unsigned long capabTimestamp = 0;
  if(millis() - capabTimestamp > 60000)
  {
    digitalWrite(13, HIGH);
    sendCapabilities();
    digitalWrite(13, LOW);
    delay(100);
    capabTimestamp = millis();
  }
}


void sendCapabilities(void)
{
    uber.sendValue("report", "rfid");    
}


void checkRFID(void)
{
  static uint32_t oldID = 0;
  static unsigned long RFIDTimestamp = 0;
  if(millis() - RFIDTimestamp > 100)
  {
    uint32_t id;

    // look for MiFare type cards
    id = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A);
    
    if (id != 0 && id!= oldID) 
    {
      uber.sendValue("rfid", "ID:" + String(id));
      oldID = id;
    }

    RFIDTimestamp = millis();
  }
  
  static unsigned long resetRFIDTimestamp = 0;
  if(millis() - resetRFIDTimestamp > 5000)
  {
    oldID = 0;
    resetRFIDTimestamp = millis();
  }
}
