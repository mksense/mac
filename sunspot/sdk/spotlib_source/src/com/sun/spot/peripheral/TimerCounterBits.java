/*
 * Copyright 2006-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.spot.peripheral;

/**
 * Timer counter bit definitions<br><br>
 * 
 * See headings in the source file for more information<br><br>
 * 
 * @see <a href="http://www.atmel.com/dyn/resources/prod_documents/doc1354.pdf">AT91 Spec</a>
 *  
 * @author John Daniels
 */
public interface TimerCounterBits {

	/*--------------------------------------------------------*/
	/* TC_CCR: Timer Counter Control Register Bits Definition */
	/*--------------------------------------------------------*/
	public static final int TC_CLKEN            = 0x1;
	public static final int TC_CLKDIS           = 0x2;
	public static final int TC_SWTRG            = 0x4;

	/*---------------------------------------------------------------*/
	/* TC_CMR: Timer Counter Channel Mode Register Bits Definition   */
	/*---------------------------------------------------------------*/

	/*-----------------*/
	/* Clock Selection */
	/*-----------------*/
	public static final int TC_CLKS                  = 0x7;
	public static final int TC_CLKS_MCK2             = 0x0;
	public static final int TC_CLKS_MCK8             = 0x1;
	public static final int TC_CLKS_MCK32            = 0x2;
	public static final int TC_CLKS_MCK128           = 0x3;
	public static final int TC_CLKS_SLCK             = 0x4;

	public static final int TC_CLKS_XC0              = 0x5;
	public static final int TC_CLKS_XC1              = 0x6;
	public static final int TC_CLKS_XC2              = 0x7;

	/*-----------------*/
	/* Clock Inversion */
	/*-----------------*/
	public static final int TC_CLKI             = 0x8;
	
	/*------------------------*/
	/* Burst Signal Selection */
	/*------------------------*/
	public static final int TC_BURST            = 0x30;
	public static final int TC_BURST_NONE       = 0x0;
	public static final int TC_BUSRT_XC0        = 0x10;
	public static final int TC_BURST_XC1        = 0x20;
	public static final int TC_BURST_XC2        = 0x30;

	/*------------------------------------------------------*/
	/* Capture Mode : Counter Clock Stopped with RB Loading */
	/*------------------------------------------------------*/
	public static final int TC_LDBSTOP          = 0x40;

	/*-------------------------------------------------------*/
	/* Waveform Mode : Counter Clock Stopped with RC Compare */
	/*-------------------------------------------------------*/
	public static final int TC_CPCSTOP          = 0x40;

	/*-------------------------------------------------------*/
	/* Capture Mode : Counter Clock Disabled with RB Loading */
	/*--------------------------------------------------------*/
	public static final int TC_LDBDIS           = 0x80;

	/*--------------------------------------------------------*/
	/* Waveform Mode : Counter Clock Disabled with RC Compare */
	/*--------------------------------------------------------*/
	public static final int TC_CPCDIS           = 0x80;

	/*------------------------------------------------*/
	/* Capture Mode : External Trigger Edge Selection */
	/*------------------------------------------------*/
	public static final int TC_ETRGEDG                  = 0x300;
	public static final int TC_ETRGEDG_EDGE_NONE        = 0x0;
	public static final int TC_ETRGEDG_RISING_EDGE      = 0x100;
	public static final int TC_ETRGEDG_FALLING_EDGE     = 0x200;
	public static final int TC_ETRGEDG_BOTH_EDGE        = 0x300;

	/*-----------------------------------------------*/
	/* Waveform Mode : External Event Edge Selection */
	/*-----------------------------------------------*/
	public static final int TC_EEVTEDG                  = 0x300;
	public static final int TC_EEVTEDG_EDGE_NONE        = 0x0;
	public static final int TC_EEVTEDG_RISING_EDGE      = 0x100;
	public static final int TC_EEVTEDG_FALLING_EDGE     = 0x200;
	public static final int TC_EEVTEDG_BOTH_EDGE        = 0x300;

	/*--------------------------------------------------------*/
	/* Capture Mode : TIOA or TIOB External Trigger Selection */
	/*--------------------------------------------------------*/
	public static final int TC_ABETRG                   = 0x400;
	public static final int TC_ABETRG_TIOB              = 0x0;
	public static final int TC_ABETRG_TIOA              = 0x400;

	/*------------------------------------------*/
	/* Waveform Mode : External Event Selection */
	/*------------------------------------------*/
	public static final int TC_EEVT                     = 0xC00;
	public static final int TC_EEVT_TIOB                = 0x0;
	public static final int TC_EEVT_XC0                 = 0x400;
	public static final int TC_EEVT_XC1                 = 0x800;
	public static final int TC_EEVT_XC2                 = 0xC00;

	/*--------------------------------------------------*/
	/* Waveform Mode : Enable Trigger on External Event */
	/*--------------------------------------------------*/
	public static final int TC_ENETRG                   = 0x1000;

	/*-----------------------------------*/
	/* Waveform Mode : RC compare action */
	/*-----------------------------------*/
	public static final int TC_WAVSEL_UP_AUTO_TRG_CP    = 2<<13;
	public static final int TC_WAVSEL_UPDOWN_NO_TRG_CP  = 1<<13;
	public static final int TC_WAVSEL_UPDOWN_AUTO_TRG_CP    = 3<<13;

	/*------------------------------------------------------------------*/
	/* Capture Mode : RC compare action - reset counter and start clock */
	/*------------------------------------------------------------------*/
	public static final int TC_CPCTRG                   = 0x4000;

	/*----------------*/
	/* Mode Selection */
	/*----------------*/
	public static final int TC_WAVE                     = 0x8000;
	public static final int TC_CAPT                     = 0x0;

	/*-------------------------------------*/
	/* Capture Mode : RA Loading Selection */
	/*-------------------------------------*/
	public static final int TC_LDRA                     = 0x30000;
	public static final int TC_LDRA_EDGE_NONE           = 0x0;
	public static final int TC_LDRA_RISING_EDGE         = 0x10000;
	public static final int TC_LDRA_FALLING_EDGE        = 0x20000;
	public static final int TC_LDRA_BOTH_EDGE           = 0x30000;

	/*-------------------------------------------*/
	/* Waveform Mode : RA Compare Effect on TIOA */
	/*-------------------------------------------*/
	public static final int TC_ACPA                     = 0x30000;
	public static final int TC_ACPA_OUTPUT_NONE         = 0x0;
	public static final int TC_ACPA_SET_OUTPUT          = 0x10000;
	public static final int TC_ACPA_CLEAR_OUTPUT        = 0x20000;
	public static final int TC_ACPA_TOGGLE_OUTPUT       = 0x30000;

	/*-------------------------------------*/
	/* Capture Mode : RB Loading Selection */
	/*-------------------------------------*/
	public static final int TC_LDRB                     = 0xC0000;
	public static final int TC_LDRB_EDGE_NONE           = 0x0;
	public static final int TC_LDRB_RISING_EDGE         = 0x40000;
	public static final int TC_LDRB_FALLING_EDGE        = 0x80000;
	public static final int TC_LDRB_BOTH_EDGE           = 0xC0000;

	/*-------------------------------------------*/
	/* Waveform Mode : RC Compare Effect on TIOA */
	/*-------------------------------------------*/
	public static final int TC_ACPC                     = 0xC0000;
	public static final int TC_ACPC_OUTPUT_NONE         = 0x0;
	public static final int TC_ACPC_SET_OUTPUT          = 0x40000;
	public static final int TC_ACPC_CLEAR_OUTPUT        = 0x80000;
	public static final int TC_ACPC_TOGGLE_OUTPUT       = 0xC0000;

	/*-----------------------------------------------*/
	/* Waveform Mode : External Event Effect on TIOA */
	/*-----------------------------------------------*/
	public static final int TC_AEEVT                    = 0x300000;
	public static final int TC_AEEVT_OUTPUT_NONE        = 0x0;
	public static final int TC_AEEVT_SET_OUTPUT         = 0x100000;
	public static final int TC_AEEVT_CLEAR_OUTPUT       = 0x200000;
	public static final int TC_AEEVT_TOGGLE_OUTPUT      = 0x300000;

	/*-------------------------------------------------*/
	/* Waveform Mode : Software Trigger Effect on TIOA */
	/*-------------------------------------------------*/
	public static final int TC_ASWTRG                   = 0xC00000;
	public static final int TC_ASWTRG_OUTPUT_NONE       = 0x0;
	public static final int TC_ASWTRG_SET_OUTPUT        = 0x400000;
	public static final int TC_ASWTRG_CLEAR_OUTPUT      = 0x800000;
	public static final int TC_ASWTRG_TOGGLE_OUTPUT     = 0xC00000;

	/*-------------------------------------------*/
	/* Waveform Mode : RB Compare Effect on TIOB */
	/*-------------------------------------------*/
	public static final int TC_BCPB                     = 0x1000000;
	public static final int TC_BCPB_OUTPUT_NONE         = 0x0;
	public static final int TC_BCPB_SET_OUTPUT          = 0x1000000;
	public static final int TC_BCPB_CLEAR_OUTPUT        = 0x2000000;
	public static final int TC_BCPB_TOGGLE_OUTPUT       = 0x3000000;

	/*-------------------------------------------*/
	/* Waveform Mode : RC Compare Effect on TIOB */
	/*-------------------------------------------*/
	public static final int TC_BCPC                     = 0xC000000;
	public static final int TC_BCPC_OUTPUT_NONE         = 0x0;
	public static final int TC_BCPC_SET_OUTPUT          = 0x4000000;
	public static final int TC_BCPC_CLEAR_OUTPUT        = 0x8000000;
	public static final int TC_BCPC_TOGGLE_OUTPUT       = 0xC000000;

	/*-----------------------------------------------*/
	/* Waveform Mode : External Event Effect on TIOB */
	/*-----------------------------------------------*/
	public static final int TC_BEEVT                    = 0x30000000;      //* bit 29-28
	public static final int TC_BEEVT_OUTPUT_NONE        = 0x0;
	public static final int TC_BEEVT_SET_OUTPUT         = 0x10000000;      //* bit 29-28  01
	public static final int TC_BEEVT_CLEAR_OUTPUT       = 0x20000000;      //* bit 29-28  10
	public static final int TC_BEEVT_TOGGLE_OUTPUT      = 0x30000000;      //* bit 29-28  11

	/*- -----------------------------------------------*/
	/* Waveform Mode : Software Trigger Effect on TIOB */
	/*-------------------------------------------------*/
	public static final int TC_BSWTRG                   = 0xC0000000;
	public static final int TC_BSWTRG_OUTPUT_NONE       = 0x0;
	public static final int TC_BSWTRG_SET_OUTPUT        = 0x40000000;
	public static final int TC_BSWTRG_CLEAR_OUTPUT      = 0x80000000;
	public static final int TC_BSWTRG_TOGGLE_OUTPUT     = 0xC0000000;

	/*------------------------------------------------------*/
	/* TC_SR: Timer Counter Status Register Bits Definition */
	/*------------------------------------------------------*/
	public static final int TC_COVFS            = 0x1;         /* Counter Overflow Status */
	public static final int TC_LOVRS            = 0x2;         /* Load Overrun Status */
	public static final int TC_CPAS             = 0x4;         /* RA Compare Status */
	public static final int TC_CPBS             = 0x8;         /* RB Compare Status */
	public static final int TC_CPCS             = 0x10;        /* RC Compare Status */
	public static final int TC_LDRAS            = 0x20;        /* RA Loading Status */
	public static final int TC_LDRBS            = 0x40;        /* RB Loading Status */
	public static final int TC_ETRGS            = 0x80;        /* External Trigger Status */
	public static final int TC_CLKSTA           = 0x10000;     /* Clock Status */
	public static final int TC_MTIOA            = 0x20000;     /* TIOA Mirror */
	public static final int TC_MTIOB            = 0x40000;     /* TIOB Status */

	/*--------------------------------------------------------------*/
	/* TC_BCR: Timer Counter Block Control Register Bits Definition */
	/*--------------------------------------------------------------*/
	public static final int TC_SYNC             = 0x1;         /* Synchronisation Trigger */

	/*------------------------------------------------------------*/
	/*  TC_BMR: Timer Counter Block Mode Register Bits Definition */
	/*------------------------------------------------------------*/
	public static final int TC_TC0XC0S          = 0x3;        /* External Clock Signal 0 Selection */
	public static final int TC_TCLK0XC0         = 0x0;
	public static final int TC_NONEXC0          = 0x1;
	public static final int TC_TIOA1XC0         = 0x2;
	public static final int TC_TIOA2XC0         = 0x3;

	public static final int TC_TC1XC1S          = 0xC;        /* External Clock Signal 1 Selection */
	public static final int TC_TCLK1XC1         = 0x0;
	public static final int TC_NONEXC1          = 0x4;
	public static final int TC_TIOA0XC1         = 0x8;
	public static final int TC_TIOA2XC1         = 0xC;

	public static final int TC_TC2XC2S          = 0x30;       /* External Clock Signal 2 Selection */
	public static final int TC_TCLK2XC2         = 0x0;
	public static final int TC_NONEXC2          = 0x10;
	public static final int TC_TIOA0XC2         = 0x20;
	public static final int TC_TIOA1XC2         = 0x30;

}
