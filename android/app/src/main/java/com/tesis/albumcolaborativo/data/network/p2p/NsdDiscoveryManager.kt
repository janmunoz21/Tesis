package com.tesis.albumcolaborativo.data.network.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class NsdDiscoveryManager(context: Context) {

    companion object {
        private const val TAG = "NsdDiscoveryManager"
        private const val SERVICE_TYPE = "_albumcolaborativo._tcp."
        private const val SERVICE_NAME_PREFIX = "AlbumNode_"
    }

    private val nsdManager: NsdManager =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val wifiManager: android.net.wifi.WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null

    private val manualNodes = mutableSetOf<ProcessingNode>()

    private val _discoveredNodes = MutableStateFlow<List<ProcessingNode>>(emptyList())
    val discoveredNodes: StateFlow<List<ProcessingNode>> = _discoveredNodes.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var currentServiceName: String? = null

    /**
     * Registra el nodo local en la red Wi-Fi mediante mDNS (CU-C-02).
     */
    fun registerService(port: Int, deviceName: String = Build.MODEL) {
        if (_isAdvertising.value) {
            Log.d(TAG, "El servicio mDNS ya está registrado.")
            return
        }

        val cleanDeviceName = formatDeviceName(deviceName)
        val serviceName = "$SERVICE_NAME_PREFIX$cleanDeviceName"
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo) {
                currentServiceName = registeredServiceInfo.serviceName
                _isAdvertising.value = true
                Log.i(TAG, "Servicio mDNS registrado con éxito: ${registeredServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isAdvertising.value = false
                Log.e(TAG, "Error al registrar servicio mDNS. Código de error: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                _isAdvertising.value = false
                Log.i(TAG, "Servicio mDNS dado de baja exitosamente.")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Error al dar de baja el servicio mDNS: $errorCode")
            }
        }

        try {
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al registrar servicio mDNS: ${e.message}")
        }
    }

    /**
     * Detiene el registro del nodo local en la red mDNS.
     */
    fun unregisterService() {
        registrationListener?.let { listener ->
            try {
                nsdManager.unregisterService(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al desregistrar mDNS: ${e.message}")
            }
            registrationListener = null
            _isAdvertising.value = false
        }
    }

    /**
     * Inicia el escaneo continuo de la subred Wi-Fi local mediante mDNS (CU-C-01).
     */
    fun startDiscovery() {
        if (_isDiscovering.value) {
            Log.d(TAG, "El escaneo mDNS ya está en marcha.")
            return
        }

        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("P2PMulticastLock").apply {
                    setReferenceCounted(true)
                    acquire()
                }
                Log.i(TAG, "MulticastLock adquirido para recepción mDNS en Wi-Fi.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al adquirir MulticastLock: ${e.message}")
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isDiscovering.value = true
                Log.i(TAG, "Iniciado escaneo mDNS para tipo: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Servicio mDNS encontrado: ${service.serviceName}")
                if (service.serviceType == SERVICE_TYPE || service.serviceType.contains("albumcolaborativo")) {
                    val myService = currentServiceName ?: ""
                    if (myService.isBlank() || !service.serviceName.startsWith(myService)) {
                        resolveService(service)
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.i(TAG, "Servicio mDNS perdido: ${service.serviceName}")
                _discoveredNodes.update { currentNodes ->
                    val filtered = currentNodes.filter { it.nodeId != service.serviceName }
                    (filtered + manualNodes).distinctBy { it.address }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isDiscovering.value = false
                Log.i(TAG, "Escaneo mDNS detenido.")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
                Log.e(TAG, "Fallo al iniciar escaneo mDNS: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Fallo al detener escaneo mDNS: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al iniciar descubrimiento mDNS: ${e.message}")
        }
    }

    /**
     * Resuelve la dirección IP y puerto del servicio descubierto.
     */
    @Suppress("DEPRECATION")
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Fallo al resolver servicio ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Servicio mDNS resuelto: ${serviceInfo.serviceName} en IP: ${serviceInfo.host}")
                val host: InetAddress? = serviceInfo.host
                val port: Int = serviceInfo.port

                if (host != null) {
                    val addressStr = host.hostAddress ?: "127.0.0.1"

                    if (isLocalIpAddress(addressStr)) {
                        Log.d(TAG, "Ignorando nodo propio ($addressStr)")
                        return
                    }

                    val rawName = serviceInfo.serviceName.removePrefix(SERVICE_NAME_PREFIX)
                    val friendlyName = formatDeviceName(rawName)

                    val newNode = ProcessingNode(
                        nodeId = serviceInfo.serviceName,
                        address = addressStr,
                        port = port,
                        available = true,
                        deviceName = friendlyName,
                        isLocal = false
                    )

                    _discoveredNodes.update { currentNodes ->
                        val filtered = currentNodes.filter { it.nodeId != newNode.nodeId }
                        (filtered + newNode + manualNodes).distinctBy { it.address }
                    }
                }
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al resolver servicio: ${e.message}")
        }
    }

    /**
     * Permite agregar manualmente un nodo por su dirección IP (útil para pruebas Emulador <-> Celular Físico).
     */
    fun addManualNode(ipAddress: String, port: Int = 50051, label: String = "Nodo Manual") {
        val newNode = ProcessingNode(
            nodeId = "Manual_$ipAddress",
            address = ipAddress,
            port = port,
            available = true,
            deviceName = label.ifBlank { "Nodo $ipAddress" },
            isLocal = false
        )
        manualNodes.add(newNode)
        _discoveredNodes.update { currentNodes ->
            val filtered = currentNodes.filter { it.address != newNode.address }
            (filtered + newNode + manualNodes).distinctBy { it.address }
        }
        Log.i(TAG, "Nodo manual agregado persistentemente: ${newNode.address}:${newNode.port}")
    }

    /**
     * Verifica si una IP dada pertenece a las interfaces locales de este teléfono.
     */
    private fun isLocalIpAddress(ip: String): Boolean {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        if (addr.hostAddress == ip) return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando IPs locales: ${e.message}")
        }
        return false
    }

    /**
     * Mapea nombres de modelo de fábrica a etiquetas amigables para el usuario.
     */
    private fun formatDeviceName(rawModel: String): String {
        val clean = rawModel.replace("_", " ").trim()
        return when {
            clean.contains("23100RN82L", ignoreCase = true) -> "Redmi 13C"
            clean.contains("one hyper", ignoreCase = true) -> "Motorola One Hyper"
            clean.startsWith("sdk", ignoreCase = true) || clean.contains("emulator", ignoreCase = true) -> "Emulador Android"
            else -> clean.ifBlank { "Dispositivo Android" }
        }
    }

    /**
     * Detiene el escaneo mDNS de la red local.
     */
    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al detener descubrimiento mDNS: ${e.message}")
            }
            discoveryListener = null
            _isDiscovering.value = false
        }
    }

    /**
     * Libera todos los recursos mDNS.
     */
    fun destroy() {
        stopDiscovery()
        unregisterService()
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando MulticastLock: ${e.message}")
        }
    }
}
