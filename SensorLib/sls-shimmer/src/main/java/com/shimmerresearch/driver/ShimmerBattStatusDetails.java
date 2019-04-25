package com.shimmerresearch.driver;

/**
 * Holds the Shimmer's Battery charging information (state, voltage and
 * percentage charge) received from communication with the Shimmer's UART.
 * 
 * @author Mark Nolan
 *
 */
public class ShimmerBattStatusDetails {
	public int mChargingStatus;
	public String mChargingStatusParsed;
	public int mBattAdcValue;
	public String mBattVoltage;
	public String mEstimatedChargePercentage;
	
	public ShimmerBattStatusDetails() {
	}

	public ShimmerBattStatusDetails(int battAdcValue, int chargeStatus) {
        boolean adcVoltageError = false;
        
        mBattAdcValue = battAdcValue;
        mChargingStatus = chargeStatus;

		// Calibration method copied from
		// com.shimmerresearch.driver.ShimmerObject.calibrateU12AdcValue
		double calibratedData=((double)battAdcValue-0.0)*(((3.0*1000.0)/1.0)/4095.0);
		//double calibratedData = calibrateU12AdcValue((double)battAdcValue, 0.0, 3.0, 1.0);
		double battVoltage = ((calibratedData * 1.988)) / 1000;
        
        if (battVoltage > 4.5) {
        	mChargingStatusParsed = "Checking...";
            adcVoltageError = true;
        }
        else if((chargeStatus & 0xFF) == 0x00) {
        	mChargingStatusParsed = "Charging suspended";
        }
        else if ((chargeStatus & 0xFF) == 0x40) {
        	mChargingStatusParsed = "Fully charged";
        }
        else if ((chargeStatus & 0xFF) == 0x80) {
            String chargingStage = "";
            if (battVoltage < 3.0) {// from lm3658 datasheet
                chargingStage = " (Preconditioning)";
            }
            //else if (battVoltage < .0)
            //{
            //    chargingStage = " (Primary charging)";
            //}
            //else if (battVoltage < .0)
            //{
            //    chargingStage = " (Conditioning)";
            //}
            else {
                chargingStage = "...";
            }

            mChargingStatusParsed = "Charging" + chargingStage;
        }
        else if ((chargeStatus & 0xFF) == 0xC0) {
        	mChargingStatusParsed = "Bad battery";
        }
        else {
        	mChargingStatusParsed = "Error";
        }

        if(adcVoltageError == false) {
        	mBattVoltage = String.format("%,.1f",battVoltage) + " V";
        	
        	// equations are only valid when: 3.2 < x < 4.167. Leaving a 0.2v either side just incase
            if (battVoltage > (4.167 + 0.2)) { 
            	battVoltage = 4.167;
            }
            else if (battVoltage < (3.2 - 0.2)) {
            	battVoltage = 3.2;
            }
        	
            double battPercentage = 0;
            // 4th order polynomial fit - good enough for purpose
            battPercentage = (1109.739792 * Math.pow(battVoltage, 4)) - (17167.12674 * Math.pow(battVoltage, 3)) + (99232.71686 * Math.pow(battVoltage, 2)) - (253825.397 * battVoltage) + 242266.0527;

            // 6th order polynomial fit - best fit -> think there is a bug with this one
            //battPercentage = -(29675.10393 * Math.pow(battVoltage, 6)) + (675893.9095 * Math.pow(battVoltage, 5)) - (6404308.2798 * Math.pow(battVoltage, 4)) + (32311485.5704 * Math.pow(battVoltage, 3)) - (91543800.1720 * Math.pow(battVoltage, 2)) + (138081754.0880 * battVoltage) - 86624424.6584;

            if (battPercentage > 100) {
                battPercentage = 100.0;
            }
            else if (battPercentage < 0) {
                battPercentage = 0.0;
            }

            if ((chargeStatus&0xFF) != 0xC0) {// Bad battery
            	mEstimatedChargePercentage = String.format("%,.1f",battPercentage) + "%";
            }
            else {
            	mEstimatedChargePercentage = "0.0%";
            }
        }
	}

}
