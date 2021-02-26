package info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.scan

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.exceptions.DiscoveredInvalidPodException
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.exceptions.ScanFailException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ScanCollector(private val logger: AAPSLogger, private val podID: Long) : ScanCallback() {

    // there could be different threads calling the onScanResult callback
    private val found: ConcurrentHashMap<String, ScanResult> = ConcurrentHashMap()
    private var scanFailed = 0
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        // callbackType will be ALL
        logger.debug(LTag.PUMPBTCOMM, "Scan found: $result")
        found[result.device.address] = result
    }

    override fun onScanFailed(errorCode: Int) {
        logger.warn(LTag.PUMPBTCOMM, "Scan failed with errorCode: $errorCode")
        super.onScanFailed(errorCode)
    }

    @Throws(ScanFailException::class) fun collect(): List<BleDiscoveredDevice> {
        val ret: MutableList<BleDiscoveredDevice> = ArrayList()
        if (scanFailed != 0) {
            throw ScanFailException(scanFailed)
        }
        logger.debug(LTag.PUMPBTCOMM, "ScanCollector looking for podID: $podID")
        for (result in found.values) {
            try {
                val device = BleDiscoveredDevice(result, podID)
                ret.add(device)
                logger.debug(LTag.PUMPBTCOMM, "ScanCollector found: " + result.toString() + "Pod ID: " + podID)
            } catch (e: DiscoveredInvalidPodException) {
                logger.debug(LTag.PUMPBTCOMM, "ScanCollector: pod not matching$e")
                // this is not the POD we are looking for
            }
        }
        return Collections.unmodifiableList(ret)
    }

}