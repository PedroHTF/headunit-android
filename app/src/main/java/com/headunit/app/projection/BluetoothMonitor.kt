package com.headunit.app.projection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Acompanha quais aparelhos Bluetooth estão conectados (eventos ACL do Android).
 *
 * Atenção: em algumas centrais genéricas o Bluetooth do telefone é um módulo à parte,
 * controlado pelo app "BT/Telefone" do fabricante, e o Android não vê as conexões.
 * Nesse caso será preciso ouvir os broadcasts do fabricante (a descobrir na central).
 */
class BluetoothMonitor(
    private val context: Context,
    private val onConnected: (BtDevice) -> Unit,
) {
    private val _connected = MutableStateFlow<List<BtDevice>>(emptyList())
    val connected: StateFlow<List<BtDevice>> = _connected.asStateFlow()

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val device = intent.device()?.let(::toBtDevice) ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> deviceConnected(device)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> deviceDisconnected(device)
            }
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }

    /** Aparelhos pareados (para associar cada um a Android Auto ou CarPlay). */
    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BtDevice> {
        if (!hasPermission()) return emptyList()
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        return runCatching { adapter.bondedDevices.orEmpty().mapNotNull(::toBtDevice) }
            .getOrDefault(emptyList())
            .sortedBy { it.name.lowercase() }
    }

    /** Usado pelo simulador e pelos eventos reais. */
    fun deviceConnected(device: BtDevice) {
        _connected.update { list -> list.filterNot { it.address == device.address } + device }
        Log.i(TAG, "Conectado: ${device.name} (${device.address})")
        onConnected(device)
    }

    fun deviceDisconnected(device: BtDevice) {
        _connected.update { list -> list.filterNot { it.address == device.address } }
    }

    @SuppressLint("MissingPermission")
    private fun toBtDevice(device: BluetoothDevice): BtDevice? = runCatching {
        BtDevice(device.address, device.name ?: device.address)
    }.getOrNull() // SecurityException sem a permissão BLUETOOTH_CONNECT

    private fun Intent.device(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    private companion object {
        const val TAG = "BluetoothMonitor"
    }
}
