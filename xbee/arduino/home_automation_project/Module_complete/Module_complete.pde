#include <NewSoftSerial.h>
#include <XbeeRadio.h>
#include <XBee.h>
#include <stdio.h>
#include <string.h>

//NewSoftSerial mySerial(2, 3);

// Create the xbee object
XBeeRadio xbee = XBeeRadio(); 

// Create reusable response objects for responses we expect to handle 
XBeeRadioResponse response = XBeeRadioResponse();  
uint8_t data = 0;

// Allocate two bytes for to hold a 32-bit analog reading
uint8_t payload[] = { 102, 0,  0, 0, 0, 0};

// With Series 1 you can use either 16-bit or 64-bit addressing

// 16-bit addressing: Enter address of remote XBee, typically the coordinator
Tx16Request tx = Tx16Request(0xffff, payload, sizeof(payload));

TxStatusResponse txStatus = TxStatusResponse();

uint8_t lampPins[] = {2, 3, 4, 5};
uint8_t relayCheckPin = A4;
uint8_t numOfRelays = 0;

uint8_t lampStatuses[5] = {0, 0, 0, 0};

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


uint8_t getNumOfRelays(void)
{
  int value = analogRead(relayCheckPin);
  int relNum = 0;
  int distance[5];
  int thresholds[] = {0, 342, 512, 614, 683, 732 };
  for(int i = 0; i< 6; i++)
  {
    thresholds[i] < value? distance[i] = value - thresholds[i] : distance[i] = thresholds[i] - value;
    Serial.print(thresholds[i], DEC);
    Serial.print("\t");
    Serial.println(distance[i], DEC);
  }
    
  for(int i = 1; i< 6; i++)
    if(distance[i] < distance[i-1]) relNum = i;
  
  return relNum;
}

void setLamp(int lamp, int value)
{
  lampStatuses[lamp] = value;
  digitalWrite(lampPins[lamp], lampStatuses[lamp]);
}

void setAllLamps(int value)
{
  for(int i = 0; i < numOfRelays ; i++)
    setLamp(i, value);

}
void setup()
{
  Serial.begin(9600);

  numOfRelays = getNumOfRelays();
  for(int i=0; i< numOfRelays; i++)
  {
    pinMode(lampPins[i], OUTPUT);   
    setLamp(i, LOW);
  }
  
  pinMode(sensorsCheckPin, INPUT);
  digitalWrite(sensorsCheckPin, HIGH);
  sensorsExist = !digitalRead(sensorsCheckPin);
  
  if(sensorsExist)
  {
    pinMode(pirPin, INPUT);
    pinMode(heaterPin, OUTPUT);
    pinMode(securityPin, INPUT);
    digitalWrite(securityPin, HIGH);
  }
  
  pinMode(ledPin, OUTPUT);
  
}

void checkLamps(void)
{
  const int inactiveMins = 5;
  static unsigned long lastTimestamp = 0;
  if(xbee.checkForData(112))
  {

    xbee.getResponse(response);
    data = response.getData(0);
    if(data == 1)
    {
      lastTimestamp = millis();
      int lamp = response.getData(1);
      int value = response.getData(2);
      if(lamp > 0 && lamp <= numOfRelays)
        setLamp(lampPins[lamp-1], value);
      else if(lamp == 0xff)
        setAllLamps(value);   
    }
  }
    
  if(millis() - lastTimestamp > inactiveMins * 60000)
    setAllLamps(LOW);  
}

void checkSensors(void)
{

}

void loop()
{
  if(numOfRelays)
    checkLamps();
    
  if(sensorsExist)
    checkSensors();
}


//// Define the variables that will be used
//unsigned long time;
//unsigned long currTime;
//unsigned long prevTime;
//unsigned long currCoPwrTimer;
//boolean CoPwrState;
//
//unsigned long timestamp = 0;
//
//unsigned long lastTimestamp = 0;
//const int inactiveMins = 5;
//
//// Calculate ppm from mq4
//float Ro_4 = 80;
//float RL_4 = 10;
//float x_t_4 = 0;
//
//// For calibration and ppm function
//float y2_4=1000.0;
//float y1_4=7000.0;
//float x2_4=1.0;
//float x1_4=0.5;
//float a_4=0;
//float b_4=0;
//int y_ppm_4 = 0;
//
//
//// Calculate ppm from mq7
//float Ro_7 = 2;
//float RL_7 = 10;
//float x_t_7 = 0;
//
//// For calibration and ppm function
//float y2_7=100.0;
//float y1_7=5000.0;
//float x2_7=1.0;
//float x1_7=0.1;
//float a_7=0;
//float b_7=0;
//int y_ppm_7 = 0;
//
//// Wiring for mq7
//int heating = 10;      // H1 connected to pin 6 for the heating pulse
//int val_7 = 0;         // variable to store the read value
//int SensorOutput_7 = A3; //analog Pin connected to "out" from sensor board (CO sensor)
//int Data_7 = 0;         //analog sensor data
//int flag=0;
//
//
//// Wiring for mq4
//int val_4 = 0;         // variable to store the read value
//int SensorOutput_4 = A2; //analog Pin connected to "out" from sensor board (CH4 sensor)
//int Data_4 = 0;         //analog sensor data
//
//// Wiring for Light Dependen Resistor
//int ldr_Pin = A1;       // select the input pin for the LDR
//int ldr_Val = 0;       // variable to store the value coming from the LIght sensor
////float ldr_Volt = 0;
////float ldr_R = 0;
////uint32_t ldr_Lux = 0;
//
//// Wiring for Passise Infrared Resistor, movement sensor
//int pir_Pin = 12;      //digital pin connected to PIR sensor "out"
//int pirState = LOW;  //variable to store the state of the PIR
//int pir_Val = 0;    //variable to store the read value 
//
//// Wiring for the relays
//int first_Lamp = 2;  //
//
//int second_Lamp = 3;  //
//
//int third_Lamp = 4;  //
//
//int fourth_Lamp = 5;  //
//
//uint8_t lamps_status[] = {0,0,0,0};
//
//
//void setup()  
//{
//
//  // set the data rate for the NewSoftSerial port
////  mySerial.begin(9600);
////  mySerial.println("BEGIN.....");
//
//  xbee.initialize_xbee_module();
//  
//  // setup xbee 
//    xbee.begin(38400);
//    xbee.init();
//  
//  //initialize variables 
//    time = 0;
//    currTime = 0;
//    prevTime = 0;
//    currCoPwrTimer = 0;
//    CoPwrState = LOW;
//    currCoPwrTimer = 500;
//    pinMode(heating, OUTPUT);   
//    
//    //find the a & b of function for mq7
//    a_7=(y2_7-y1_7)/(x2_7-x1_7);
//    b_7=y2_7-a_7*x2_7;
//  
//  
//    //find the a & b of function for mq4
//    a_4=(y2_4-y1_4)/(x2_4-x1_4);
//    b_4=y2_4-a_4*x2_4;
//    
//    pinMode(ldr_Pin, INPUT);  //Declare the LDR as INPUT
//    
//    pinMode(pir_Pin, INPUT);  //Declare the PIR sensor as INPUT
////    digitalWrite(pir_Pin, HIGH); //Enable the internal(Arduino's) pullup resistor
//    
//    pinMode(first_Lamp, OUTPUT);  //Declare first_Lamp as OUTPOUT
//    digitalWrite(first_Lamp, LOW);  //Set first_Lamp low, for initialisations
//    pinMode(second_Lamp, OUTPUT);  //Declare second_Lamp as OUTPOUT
//    digitalWrite(second_Lamp, LOW);  //Set second_Lamp low, for initialisations
//    pinMode(third_Lamp, OUTPUT);  //Declare third_Lamp as OUTPOUT
//    digitalWrite(third_Lamp, LOW);  //Set third_Lamp low, for initialisations
//    pinMode(fourth_Lamp, OUTPUT);  //Declare fourth_Lamp as OUTPOUT
//    digitalWrite(fourth_Lamp, LOW);  //Set fourth_Lamp low, for initialisations
//    
//    pinMode(13, OUTPUT);
//    
//}  // End setup
// 
//
//void loop()
//{
//  
////  digitalWrite(13, HIGH);
////  delay(50);
////  digitalWrite(13, LOW);
////  delay(50);
//  
//  /****************************
//   ***** VALUES CALCULATIONS **
//   ****************************/
//
//   // for light sensor
//  ldr_Val = analogRead(ldr_Pin);  // read the value from the sensor
//  
//  static unsigned long pirTimestamp = 0;
//  if(millis() - pirTimestamp > 500)
//  {
//    
//    int newPirVal = digitalRead(pir_Pin); // read the value from the sensor
//    if(newPirVal != pir_Val)
//    {
////      send_data(7, newPirVal);
//      payload[1] = 7;
//      payload[2] = !newPirVal;
//      payload[3] = !newPirVal;
//      payload[4] = !newPirVal;
//      payload[5] = !newPirVal;
//      send_data();
//
//    }
//    pir_Val = newPirVal;
//    pirTimestamp = millis();
//
//  // for mq4
//  Data_4 = analogRead(SensorOutput_4);
//    
//  CoPwrCycler();  
//  analogWrite(heating, 255);
//   
//  if(CurrentState() == LOW){   //we are at 1.4v, read sensor data!
//           analogWrite(heating, 71);  
//           Data_7 = analogRead(SensorOutput_7);
//           flag=1;
//         }
//  
//    print_data();
//
//    if(millis() - timestamp > 5000)
//    {
//    send_data(1, ldr_Val);
////    send_data(7, pir_Val);
//    
//    payload[1] = 7;
//    payload[2] = !pir_Val;
//    payload[3] = !pir_Val;
//    payload[4] = !pir_Val;
//    payload[5] = !pir_Val;
//    send_data();
//    
//    send_data(4, y_ppm_7);
//    send_data(6, y_ppm_4);
//    for(int tmp = 1; tmp <= 4; tmp++)
//    {
//      payload[1] = 14;
//      payload[2] = tmp;
//      payload[3] = lamps_status[tmp-1];
//      send_data();
//    }    
//    
//    timestamp = millis();
//    }
//   
//  /****************************
//   ***** CONTROL COMMANDS *****
//   ****************************/
//  if(xbee.checkForData(112))
//  {
//
//    xbee.getResponse(response);
//	  data = response.getData(0);
//      if(data == 1)
//      {
//        lastTimestamp = millis();
//        int lamp = response.getData(1);
//        if(lamp == 1)
//        {
//          digitalWrite(first_Lamp, response.getData(2));
//        }else if(lamp == 2)
//        {
//           digitalWrite(second_Lamp, response.getData(2));
//        }else if(lamp == 3)
//        {
//           digitalWrite(third_Lamp, response.getData(2)); 
//        }else if(lamp == 4)
//        {
//           digitalWrite(fourth_Lamp, response.getData(2)); 
//        }
//        else if(lamp == 0xff)
//        {
//          digitalWrite(first_Lamp, response.getData(2));
//          digitalWrite(second_Lamp, response.getData(2));
//          digitalWrite(third_Lamp, response.getData(2)); 
//          digitalWrite(fourth_Lamp, response.getData(2));
//       
//          lamps_status[0]=response.getData(2);       
//          lamps_status[1]=response.getData(2);       
//          lamps_status[2]=response.getData(2);       
//          lamps_status[3]=response.getData(2);       
//        }
//       if(lamp > 0 && lamp < 5) 
//         lamps_status[lamp-1]=response.getData(2);    
//      }
//    }
//    
//        if(millis() - lastTimestamp > inactiveMins * 60000)
//    {
//      digitalWrite(first_Lamp, LOW);
//      digitalWrite(second_Lamp, LOW);
//      digitalWrite(third_Lamp, LOW);
//      digitalWrite(fourth_Lamp, LOW);
//      
//      lamps_status[0]=LOW;       
//      lamps_status[1]=LOW;       
//      lamps_status[2]=LOW;       
//      lamps_status[3]=LOW;   
//    }
//
//}// End loop 
//
//
//
//
//
////Subroutine, which sends sensor's data
//void send_data(int sensor_type, int sensor_val)
//{
//      // initialize bla_pointer pointer for reading 32 bit sensors values
//    uint8_t * bla_pointer;
//       
//    payload[1] = sensor_type;
//    bla_pointer = (uint8_t*) &sensor_val; //ldr_val
//    payload[2] = *bla_pointer;
//    bla_pointer++;
//    payload[3] = *bla_pointer;
//    bla_pointer++;
//    payload[4] = 0; //*bla_pointer;
//    bla_pointer++;
//    payload[5] = 0; //*bla_pointer;
//    
////    mySerial.println(ldr_Val);  // Prints the values on the extra serial (for testing purposes)
//    send_data();
//
//}
//
//void send_data(void)
//{
//    xbee.send(tx,112);
//    // after sending a tx request, we expect a status response
//    // wait up to 5 seconds for the status response
//    if (xbee.readPacket(5000)) 
//    {
//        // got a response!
//
//        // should be a znet tx status            	
//    	if (xbee.getResponse().getApiId() == TX_STATUS_RESPONSE) 
//        {
//    	   xbee.getResponse().getZBTxStatusResponse(txStatus);
//    		
//    	   // get the delivery status, the fifth byte
//           if (txStatus.getStatus() == SUCCESS) 
//           {
//            	// success.  time to celebrate
//           } else 
//           {
//            	// the remote XBee did not receive our packet. is it powered on?
//           }
//        }      
//    } else 
//    {
//      // local XBee did not provide a timely TX Status Response -- should not happen
//
//    }
//    delay(100);  
//}
//
////Subroutine, which calculates the Parts Per Million for the Gas Sensors
//void print_data(){
//    
//    float Vrl_4 = (5.0 * Data_4) / 1023;
//    float  Rs_4 = RL_4 * ( 5.0 - Vrl_4) / Vrl_4;
//    x_t_4 = Rs_4/Ro_4;
//    y_ppm_4 = a_4*x_t_4+b_4;
//    
//    float Vrl_7 = (5.0 * Data_7) / 1023;
//    float  Rs_7 = RL_7 * ( 5.0 - Vrl_7) / Vrl_7;
//    x_t_7 = Rs_7/Ro_7;
//    y_ppm_7 = a_7*x_t_7+b_7;
//  }
//
////Subroutine , which provides the necessary pulse for the CO (MQ-7) gas sensor
//void CoPwrCycler(){
//  
//  currTime = millis();
//   
//  if (currTime - prevTime > currCoPwrTimer){
//    prevTime = currTime;
//    
//     if(CoPwrState == LOW){
//      CoPwrState = HIGH;
//      currCoPwrTimer = 15000;  //15 seconds at 5v
//    }
//    else{
//      CoPwrState = LOW;
//      currCoPwrTimer = 30000;  //30 seconds at 1.4v
//    
//    }
//  }
//}
//
////
//boolean CurrentState()
//{
//  if(CoPwrState == LOW){
//		return false;
//	}
//	else{
//		return true;
//	}
//}
//
//
//char * floatToString(char * outstr, double val, byte precision, byte widthp){
//  char temp[16]; //increase this if you need more digits than 15
//  byte i;
//
//  temp[0]='\0';
//  outstr[0]='\0';
//
//  if(val < 0.0){
//    strcpy(outstr,"-\0");  //print "-" sign
//    val *= -1;
//  }
//
//  if( precision == 0) {
//    strcat(outstr, ltoa(round(val),temp,10));  //prints the int part
//  }
//  else {
//    unsigned long frac, mult = 1;
//    byte padding = precision-1;
//    
//    while (precision--)
//      mult *= 10;
//
//    val += 0.5/(float)mult;      // compute rounding factor
//    
//    strcat(outstr, ltoa(floor(val),temp,10));  //prints the integer part without rounding
//    strcat(outstr, ".\0"); // print the decimal point
//
//    frac = (val - floor(val)) * mult;
//
//    unsigned long frac1 = frac;
//
//    while(frac1 /= 10)
//      padding--;
//
//    while(padding--)
//      strcat(outstr,"0\0");    // print padding zeros
//
//    strcat(outstr,ltoa(frac,temp,10));  // print fraction part
//  }
//
//  // generate width space padding
//  if ((widthp != 0)&&(widthp >= strlen(outstr))){
//    byte J=0;
//    J = widthp - strlen(outstr);
//
//    for (i=0; i< J; i++) {
//      temp[i] = ' ';
//    }
//
//    temp[i++] = '\0';
//    strcat(temp,outstr);
//    strcpy(outstr,temp);
//  }
//
//  return outstr;
//}
