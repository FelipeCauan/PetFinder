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

/**
 * Helper offline-safe para captura de localização via LocationManager e geocodificação opcional.
 */
class LocationHelper(context: Context) {

    private val appContext = context.applicationContext

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentAddress(): Result<CapturedAddress> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Permissão de localização não concedida."))
        }

        val locationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return Result.failure(IllegalStateException("Serviço de localização indisponível."))

        val location = fetchBestLocation(locationManager)
            ?: return Result.failure(
                IllegalStateException("Não foi possível obter a localização. Verifique se o GPS está ativo.")
            )

        val geocoded = reverseGeocode(location.latitude, location.longitude)
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

    @SuppressLint("MissingPermission")
    private suspend fun fetchBestLocation(locationManager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        val activeProvider = providers.firstOrNull { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        if (activeProvider != null && activeProvider != LocationManager.PASSIVE_PROVIDER) {
            val fresh = withTimeoutOrNull(4500L) {
                requestSingleCurrentLocation(locationManager, activeProvider)
            }
            if (fresh != null) return fresh
        }

        return providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleCurrentLocation(
        locationManager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }

        runCatching {
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(appContext)
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
        }.onFailure {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double): Address? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(appContext, Locale("pt", "BR"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(2500L) {
                    suspendCancellableCoroutine { continuation ->
                        runCatching {
                            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                                if (continuation.isActive) {
                                    continuation.resume(addresses.firstOrNull())
                                }
                            }
                        }.onFailure {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
            }
        }

    private fun normalizeUf(adminArea: String?): String? {
        if (adminArea.isNullOrBlank()) return null
        val trimmed = adminArea.trim()
        if (trimmed.length == 2) return trimmed.uppercase()
        return BRAZIL_STATE_TO_UF[trimmed.lowercase()]
    }

    companion object {
        private val BRAZIL_STATE_TO_UF = mapOf(
            "acre" to "AC",
            "alagoas" to "AL",
            "amapá" to "AP",
            "amapa" to "AP",
            "amazonas" to "AM",
            "bahia" to "BA",
            "ceará" to "CE",
            "ceara" to "CE",
            "distrito federal" to "DF",
            "espírito santo" to "ES",
            "espirito santo" to "ES",
            "goiás" to "GO",
            "goias" to "GO",
            "maranhão" to "MA",
            "maranhao" to "MA",
            "mato grosso" to "MT",
            "mato grosso do sul" to "MS",
            "minas gerais" to "MG",
            "pará" to "PA",
            "para" to "PA",
            "paraíba" to "PB",
            "paraiba" to "PB",
            "paraná" to "PR",
            "parana" to "PR",
            "pernambuco" to "PE",
            "piauí" to "PI",
            "piaui" to "PI",
            "rio de janeiro" to "RJ",
            "rio grande do norte" to "RN",
            "rio grande do sul" to "RS",
            "rondônia" to "RO",
            "rondonia" to "RO",
            "roraima" to "RR",
            "santa catarina" to "SC",
            "são paulo" to "SP",
            "sao paulo" to "SP",
            "sergipe" to "SE",
            "tocantins" to "TO"
        )
    }
}
