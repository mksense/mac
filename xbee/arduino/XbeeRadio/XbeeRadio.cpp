/*
XBeeRadio.h - Library for communicating with heterogenous 802.15.4 networks.
	Created by Vasileios Georgitzikis, November 23, 2010.
*/

#include "WProgram.h"
#include "XbeeRadio.h"

//XBeeRadioResponse::XBeeRadioResponse() : XBeeResponse(){}
	bool XBeeRadioResponse::validPacket(uint8_t lp1,uint8_t lp2,uint8_t port)
{
	Rx16Response response = Rx16Response();
	uint8_t ApiId = XBeeResponse::getApiId();
	if(ApiId == RX_16_RESPONSE)
	{
		getRx16Response(response);
	//uint8_t myData = response.getData(0);
		if(response.getData(0) == lp1 && response.getData(1) == lp2 && response.getData(2) == port)
		{
			return true;
		}
		else
			return false;
	}
}

uint8_t XBeeRadioResponse::getDataLength()
{
	Rx16Response response = Rx16Response();
	getRx16Response(response);
	return response.getDataLength()-3;
}
uint8_t XBeeRadioResponse::getData(int index)
{
	Rx16Response response = Rx16Response();
	getRx16Response(response);
	return response.getData(index+3);
}
uint8_t* XBeeRadioResponse::getData()
{
	Rx16Response response = Rx16Response();
	getRx16Response(response);
	return response.getData()+3;
}
bool XBeeRadioResponse::validPacket()
{
	return validPacket(LP1, LP2, PORT);
}
bool XBeeRadioResponse::validPacket(uint8_t valid_port)
{
	return validPacket(LP1, LP2, valid_port);
}

uint8_t XBeeRadioResponse::getRssi() {
	return getFrameData()[2];
}

//XBeeRadio::XBeeRadio() : XBee(){}
void 	XBeeRadio::getResponse(XBeeRadioResponse &response)
{
	XBee::getResponse(response);
}
XBeeRadioResponse& XBeeRadio::getResponse()
{
	return (XBeeRadioResponse&) XBee::getResponse();
}
void XBeeRadio::send(Tx16Request &request)
{
	/*
	uint8_t *temp_payload = request.getPayload();
	uint8_t payloadLength = request.getPayloadLength();
	uint8_t *new_payload = (uint8_t*) malloc(sizeof(uint8_t) * (payloadLength+3));

	for(int i=0;i<payloadLength;i++)
	{
		new_payload[i+3] = temp_payload[i];
	}
	new_payload[0] = 0x7f;
	new_payload[1] = 0x69;
	new_payload[2] = 110;
	
	request.setPayload(new_payload);
	request.setPayloadLength(payloadLength+3);
		
	XBee::send(request);

	delay(10);	
	
	request.setPayload(temp_payload);
	request.setPayloadLength(payloadLength);
	
	free(new_payload);
	*/
	send(request, 110);
}

void XBeeRadio::send(Tx16Request &request, uint8_t port)
{
	uint8_t *temp_payload = request.getPayload();
	uint8_t payloadLength = request.getPayloadLength();
	uint8_t *new_payload = (uint8_t*) malloc(sizeof(uint8_t) * (payloadLength+3));

	for(int i=0;i<payloadLength;i++)
	{
		new_payload[i+3] = temp_payload[i];
	}
	new_payload[0] = 0x7f;
	new_payload[1] = 0x69;
	new_payload[2] = port;
	
	request.setPayload(new_payload);
	request.setPayloadLength(payloadLength+3);
		
	XBee::send(request);

	delay(10);	
	
	request.setPayload(temp_payload);
	request.setPayloadLength(payloadLength);
	
	free(new_payload);
}

void XBeeRadio::send(AtCommandRequest request)
{
	XBee::send(request);
}
uint8_t XBeeRadio::init(void)
{
	delay(1000);
	//XBeeRadio temp_xbee = XBeeRadio();
// serial low
	uint8_t slCmd[] = {'S','L'};

	uint8_t chCmd[] = {'C','H'};
	uint8_t chValue[] = {0x0C};

	uint8_t pidCmd[] = {'I','D'};
	uint8_t pidValue[] = {0x01};

	uint8_t apCmd[] = {'A','P'};
	uint8_t apValue[] = {0x02};

	uint8_t mmCmd[] = {'M','M'};
	uint8_t mmValue[] = {0x02};

	uint8_t myCmd[] = {'M','Y'};
	uint8_t myValue[] = {0x00, 0x0A};

	AtCommandRequest atRequest = AtCommandRequest(slCmd);

	AtCommandResponse atResponse = AtCommandResponse();

// get SH
	uint8_t buffer[] = {0x00, 0x0B};
	sendAtCommand(buffer, atRequest, atResponse);

//set MY
	
	myAddress = buffer[1];
	myAddress <<= 8;
	myAddress += buffer[0];
	
	myValue[0] = buffer[0];
	myValue[1] = buffer[1];
	atRequest.setCommand(myCmd);  
	atRequest.setCommandValue(myValue);
	atRequest.setCommandValueLength(sizeof(myValue));
	sendAtCommand(buffer, atRequest, atResponse);
	atRequest.clearCommandValue();

// set CH
	atRequest.setCommand(chCmd);  
	atRequest.setCommandValue(chValue);
	atRequest.setCommandValueLength(sizeof(chValue));
	sendAtCommand(buffer, atRequest, atResponse);
	atRequest.clearCommandValue();

// set PID
	atRequest.setCommand(pidCmd);  
	atRequest.setCommandValue(pidValue);
	atRequest.setCommandValueLength(sizeof(pidValue));
	sendAtCommand(buffer, atRequest, atResponse);
	atRequest.clearCommandValue();

// set AP
	atRequest.setCommand(apCmd);  
	atRequest.setCommandValue(apValue);
	atRequest.setCommandValueLength(sizeof(apValue));
	sendAtCommand(buffer, atRequest, atResponse);
	atRequest.clearCommandValue();

// set MM
	atRequest.setCommand(mmCmd);  
	atRequest.setCommandValue(mmValue);
	atRequest.setCommandValueLength(sizeof(mmValue));
	sendAtCommand(buffer, atRequest, atResponse);
	atRequest.clearCommandValue();
}
uint8_t XBeeRadio::sendAtCommand(uint8_t buffer[2], AtCommandRequest atRequest,AtCommandResponse atResponse)
{
	uint8_t error=0;

// send the command
	this->send(atRequest);

// wait up to 5 seconds for the status response
	if (this->readPacket(5000))
	{
	// should be an AT command response
		if (this->getResponse().getApiId() == AT_COMMAND_RESPONSE)
		{
			this->getResponse().getAtCommandResponse(atResponse);

			if (atResponse.isOk())
			{
				if (atResponse.getValueLength() > 0)
				{
					buffer[0] = atResponse.getValue()[2];
					buffer[1] = atResponse.getValue()[3];
				}
			} 
			else
			{
				error=1;
			}
		} 
		else
		{
			error=1;
		}   
	} 
	else
	{
		error=1;
	}

	return error;
}
uint16_t XBeeRadio::getMyAddress()
{
	return this->myAddress;
}


