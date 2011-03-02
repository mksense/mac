/*
  XBeeRadio.h - Library for communicating with heterogenous 802.15.4 networks.
  Created by Vasileios Georgitzikis, November 23, 2010.
*/

#ifndef XbeeRadio_h
#define XbeeRadio_h

#include <Xbee.h>
#include "WProgram.h"

class XBeeRadioResponse : public XBeeResponse
{
public:
	const static uint8_t LP1  = 0x7f;
	const static uint8_t LP2  = 0x69;
	const static uint8_t PORT = 110;
	XBeeRadioResponse() : XBeeResponse(){}
	bool validPacket(uint8_t lp1,uint8_t lp2,uint8_t port);
	uint8_t getDataLength();
	uint8_t getData(int index);
	uint8_t* getData();
	bool validPacket();
	bool validPacket(uint8_t valid_port);
	uint8_t getRssi();
};

class XBeeRadio: public XBee
{
public:
	XBeeRadio() : XBee(){}
	void getResponse(XBeeRadioResponse &response);
	XBeeRadioResponse& getResponse();
	uint16_t myAddress;
	uint16_t getMyAddress();
	void send(Tx16Request &request);
	void send(Tx16Request &request, uint8_t port);
	void send(AtCommandRequest);
	uint8_t sendAtCommand(uint8_t buffer[2], AtCommandRequest atRequest,AtCommandResponse atResponse);
	uint8_t init();
};

#endif


