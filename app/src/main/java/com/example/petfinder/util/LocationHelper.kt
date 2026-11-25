package com.example.petfinder.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CapturedAddress(
    val latitude: Double,
    val longitude: Double,
    val cep: String? = null,
    val street: String? = null,
    val number: String? = null,
    val city: String? = null,
    val uf: String? = null
)

class LocationHelper(context: Context) {

    private val appContext = context.applicationContext

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentAddress(): Result<CapturedAddress> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Permissão de localização não concedida."))
        }
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.failure(IllegalStateException("Serviço de localização indisponível."))

        val location = fetchBestLocation(locationManager)
            ?: return Result.failure(IllegalStateException("Não foi possível obter a localização. Verifique se o GPS está ativo."))

        @Suppress("DEPRECATION")
        val geocoded = queryGeocoder { it.getFromLocation(location.latitude, location.longitude, 1) }

        return Result.success(
            CapturedAddress(
                latitude = location.latitude,
                longitude = location.longitude,
                cep = geocoded?.postalCode?.filter { it.isDigit() }?.take(8),
                street = geocoded?.thoroughfare,
                number = geocoded?.subThoroughfare,
                city = geocoded?.locality ?: geocoded?.subAdminArea,
                uf = normalizeUf(geocoded?.adminArea)
            )
        )
    }

    suspend fun forwardGeocode(addressText: String): Pair<Double, Double>? {
        val trimmed = addressText.trim()
        if (trimmed.isBlank()) return null
        @Suppress("DEPRECATION")
        val address = queryGeocoder { it.getFromLocationName(trimmed, 1) }
        return address?.let { it.latitude to it.longitude }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchBestLocation(locationManager: LocationManager): Location? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val activeProvider = providers.firstOrNull {
            runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (activeProvider != null) {
            val fresh = withTimeoutOrNull(4500L) {
                suspendCancellableCoroutine { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    runCatching {
                        LocationManagerCompat.getCurrentLocation(
                            locationManager,
                            activeProvider,
                            signal,
                            ContextCompat.getMainExecutor(appContext)
                        ) { loc -> if (cont.isActive) cont.resume(loc) }
                    }.onFailure { if (cont.isActive) cont.resume(null) }
                }
            }
            if (fresh != null) return fresh
        }

        return (providers + LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private suspend fun queryGeocoder(block: (Geocoder) -> List<Address>?): Address? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(appContext, Locale.forLanguageTag("pt-BR"))
            runCatching { block(geocoder)?.firstOrNull() }.getOrNull()
        }

    private fun normalizeUf(adminArea: String?): String? {
        val trimmed = adminArea?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.length == 2) return trimmed.uppercase()
        val key = Normalizer.normalize(trimmed.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return BRAZIL_STATE_TO_UF[key]
    }

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0

        fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
        }

        private val BRAZIL_STATE_TO_UF = mapOf(
            "acre" to "AC", "alagoas" to "AL", "amapa" to "AP", "amazonas" to "AM",
            "bahia" to "BA", "ceara" to "CE", "distrito federal" to "DF", "espirito santo" to "ES",
            "goias" to "GO", "maranhao" to "MA", "mato grosso" to "MT", "mato grosso do sul" to "MS",
            "minas gerais" to "MG", "para" to "PA", "paraiba" to "PB", "parana" to "PR",
            "pernambuco" to "PE", "piaui" to "PI", "rio de janeiro" to "RJ",
            "rio grande do norte" to "RN", "rio grande do sul" to "RS", "rondonia" to "RO",
            "roraima" to "RR", "santa catarina" to "SC", "sao paulo" to "SP",
            "sergipe" to "SE", "tocantins" to "TO"
        )
    }
}
