package com.example.petfinder.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

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

        val geocoded = queryGeocoder(
            modernCall = { geocoder, callback -> geocoder.getFromLocation(location.latitude, location.longitude, 1, callback) },
            legacyCall = { geocoder ->
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            }
        )

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
        val address = queryGeocoder(
            modernCall = { geocoder, callback -> geocoder.getFromLocationName(trimmed, 1, callback) },
            legacyCall = { geocoder ->
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(trimmed, 1)
            }
        )
        return address?.let { it.latitude to it.longitude }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchBestLocation(locationManager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val activeProvider = providers.firstOrNull {
            runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (activeProvider != null && activeProvider != LocationManager.PASSIVE_PROVIDER) {
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

        return providers
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private suspend fun queryGeocoder(
        modernCall: (Geocoder, Geocoder.GeocodeListener) -> Unit,
        legacyCall: (Geocoder) -> List<Address>?
    ): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(appContext, Locale("pt", "BR"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(2500L) {
                suspendCancellableCoroutine { cont ->
                    runCatching {
                        modernCall(geocoder) { list -> if (cont.isActive) cont.resume(list.firstOrNull()) }
                    }.onFailure { if (cont.isActive) cont.resume(null) }
                }
            }
        } else {
            runCatching { legacyCall(geocoder)?.firstOrNull() }.getOrNull()
        }
    }

    private fun normalizeUf(adminArea: String?): String? {
        val trimmed = adminArea?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.length == 2) return trimmed.uppercase()
        return BRAZIL_STATE_TO_UF[trimmed.lowercase()]
    }

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0

        fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
            return EARTH_RADIUS_KM * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        }

        private val BRAZIL_STATE_TO_UF = mapOf(
            "acre" to "AC", "alagoas" to "AL", "amapá" to "AP", "amapa" to "AP",
            "amazonas" to "AM", "bahia" to "BA", "ceará" to "CE", "ceara" to "CE",
            "distrito federal" to "DF", "espírito santo" to "ES", "espirito santo" to "ES",
            "goiás" to "GO", "goias" to "GO", "maranhão" to "MA", "maranhao" to "MA",
            "mato grosso" to "MT", "mato grosso do sul" to "MS", "minas gerais" to "MG",
            "pará" to "PA", "para" to "PA", "paraíba" to "PB", "paraiba" to "PB",
            "paraná" to "PR", "parana" to "PR", "pernambuco" to "PE", "piauí" to "PI",
            "piaui" to "PI", "rio de janeiro" to "RJ", "rio grande do norte" to "RN",
            "rio grande do sul" to "RS", "rondônia" to "RO", "rondonia" to "RO",
            "roraima" to "RR", "santa catarina" to "SC", "são paulo" to "SP",
            "sao paulo" to "SP", "sergipe" to "SE", "tocantins" to "TO"
        )
    }
}
