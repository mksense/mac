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
 * Interface to an AT91 Timer Counter.
 *<p>
 * The AT91 Timer Counter includes three identical 16-bit Timer Counter channels.
 *<p>
 * Each channel can be independently programmed to perform a wide range of functions including
 * frequency measurement, event counting, interval measurement, pulse generation, delay timing
 * and pulse width modulation.
 *<p>
 * Each channel has three external clock inputs, five internal clock inputs and two multi-purpose
 * input/output signals which can be configured by the user. Each channel drives an internal interrupt
 * signal which can be programmed to generate processor interrupts.
 *<p>
 * For a full description of the AT91 Timer Counter please refer to the Atmel documentation.
 * Here we will provide a few basics to allow using the Timer to generate a periodic interrupt.
 * For Timer Counter bit definitions please see {@link TimerCounterBits}.
 *<p>
 * Several signals are provided on the SPOT processor board via the top connector, though the 
 * initial eDemo sensor board does not make them available. The available signals are the three
 * external clock inputs (TCLK0, TCLK1, TCLK2) and the general purpose input/output pins for 
 * channel 0 (TIOA0, TIOB0).
 *<p>
 * The Timer Counter can operate in two distinct modes: Capture & Waveform generation. 
 * In either mode the rate at which the Timer counts is determined by which internal clock is
 * used. The available clock speeds are:
 *<ul>
 *<li> 29,952 Khz:  One count = 0.0334 usec. Max duration = 2.188 msec. ({@link TimerCounterBits#TC_CLKS_MCK2})
 *<li> 7,488 Khz:  One count = 0.1335 usec. Max duration = 8.752 msec. ({@link TimerCounterBits#TC_CLKS_MCK8})
 *<li> 1,872 Khz:  One count = 0.5342 usec. Max duration = 35.009 msec. ({@link TimerCounterBits#TC_CLKS_MCK32})
 *<li>  468 Khz:  One count = 2.1368 usec. Max duration = 140.034 msec. ({@link TimerCounterBits#TC_CLKS_MCK128})
 *<li>  32.768 Khz:  One count = 30.5176 usec. Max duration = 2,000 msec = 2 sec. ({@link TimerCounterBits#TC_CLKS_SLCK})
 *</ul>
 *<p>
 * To use the Timer to measure an interval use Capture Mode, enable the clock to start it counting, 
 * and at the end of the interval just read the counter value:
 *<code>
 * <pre>
 *    IAT91_TC timer = Spot.getInstance().getAT91_TC(0);
 *    timer.configure(TC_CAPT | TC_CLKS_MCK32);
 *    timer.enableAndReset(); 
 *    ... interval to measure ...
 *    int cntr = timer.counter();
 *    double interval = cntr * 0.5342;  // time in microseconds
 * </pre>
 *</code>
 * To generate periodic interrupts modify the above code to set the RC Register to the number of counts
 * that will span the desired period, and enable interrupts on RC Compare:
 *<code>
 * <pre>
 *    IAT91_TC timer = Spot.getInstance().getAT91_TC(0);
 *    int cnt = (int)(25000 / 0.5342);  // number of clock counts for 25 milliseconds
 *    timer.configure(TC_CAPT | TC_CPCTRG | TC_CLKS_MCK32);
 *    timer.setRegC(cnt);
 *    timer.enableAndReset();
 *    while(true) {
 *        timer.enableIrq(TC_CPCS);     // Enable RC Compare interrupt
 *        timer.waitForIrq();
 *        timer.status();               // Clear interrupt pending flag
 *        doTask();                     // method will be called every 25 milliseconds
 *    }
 * </pre>
 *</code>
 * A Java thread detects an interrupt by calling {@link IAT91_TC#waitForIrq()}.
 * This method performs a Channel IO request. If the Timer Counter interrupt bit
 * is set the request returns immediately. If not the calling thread is blocked 
 * until the interrupt occurs. To clear the interrupt bit call {@link IAT91_TC#status()}.
 *<p>
 * To handle an interrupt in Java you must:
 *<ol>
 *<li> Call {@link IAT91_TC#configure(int)} to configure the timer so that it will generate an interrupt request.
 *<li> Call {@link IAT91_TC#enableAndReset()} to start the timer counting
 *<li> Call {@link IAT91_TC#enableIrq(int)} to enable one or more of the interrupt sources associated with this TC channel.
 *<li> Call {@link IAT91_TC#waitForIrq()} to wait for the interrupt.
 *<li> Call {@link IAT91_TC#status()} to clear the interrupt.
 *<li> Call {@link IAT91_TC#enableIrq(int)} to allow another interrupt.
 *<li> Repeat from 4)
 *</ol>
 *<p>
 * If you don't want more interrupts then don't call {@link IAT91_TC#enableIrq(int)}.
 *<p>
 *
 * @see <a href="http://www.atmel.com/dyn/resources/prod_documents/doc1354.pdf">AT91 Spec</a>
 *
 * @author Syntropy
 */
public interface IAT91_TC extends IDriver {
	/**
	 * Configure the Timer-Counter
	 * @param mask bits to set in TC_CMR
	 */
	public abstract void configure(int mask);

	/**
	 * Enable counter and cause software trigger which forces a reset on next clock edge
	 */
	public abstract void enableAndReset();

	/**
	 * Enable counter
	 */
	public abstract void enable();

	/**
	 * Read current counter value
	 * @return TC_CV
	 */
	public abstract int counter();

	/**
	 * Read current status
	 * @return TC_SR
	 */
	public abstract int status();

	/**
	 * Disable PIO use of shared TIOA line
	 */
	public abstract void claimTIOA();

	/**
	 * Enable PIO use of shared TIOA line
	 */
	public abstract void unclaimTIOA();

	/**
	 * Disable PIO use of shared TIOB line
	 */
	public abstract void claimTIOB();

	/**
	 * Enable PIO use of shared TCLK line
	 */
	public abstract void claimTCLK();

	/**
	 * Read current Reg A value
	 * @return TC_RA
	 */
	public abstract int regA();

	/**
	 * Read current Reg B value
	 * @return TC_RB
	 */
	public abstract int regB();

	/**
	 * Set the value of the C Compare Register
	 * @param i Value to be set
	 */
	public abstract void setRegC(int i);

	/**
	 * Set the value of the A Compare Register
	 * @param i Value to be set
	 */
	public abstract void setRegA(int i);

	/**
	 * Disable the counter
	 */
	public abstract void disable();

	/**
	 * Configure the block mode inputs appropriate to this TC channel
	 * @param xcMask Bits to be set in TC_BMR
	 */
	public abstract void configureXC(int xcMask);

	/**
	* Perform a block sync command, which causes a reset of all three counters
	*/
	public abstract void  blockSync();
	 
	/**
	 * Enable one or more of the interrupt sources associated with this TC channel.
	 * @param mask Bit mask identifying the required interrupt source(s)
	 */
	public abstract void enableIrq(int mask);

	/**
	 * Suspend this thread until this TC generates an interrupt.
	 * You must call {@link #enableIrq(int)} prior to this, and again if you want a subsequent interrupt.
	 */
	public abstract void waitForIrq();

	/**
	 * Disable one or more of the interrupt sources associated with this TC channel.
	 * @param mask Bit mask identifying the required interrupt source(s)
	 */
	public abstract void disableIrq(int mask);
}
