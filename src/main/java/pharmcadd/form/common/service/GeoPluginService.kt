package pharmcadd.form.common.service

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

data class GeoInfo(
    @field:JsonProperty("geoplugin_request")
    val request: String,
    @field:JsonProperty("geoplugin_status")
    val status: Int,
    @field:JsonProperty("geoplugin_delay")
    val delay: String,
    @field:JsonProperty("geoplugin_credit")
    val credit: String,
    @field:JsonProperty("geoplugin_city")
    val city: String?,
    @field:JsonProperty("geoplugin_region")
    val region: String?,
    @field:JsonProperty("geoplugin_regionCode")
    val regionCode: String?,
    @field:JsonProperty("geoplugin_regionName")
    val regionName: String?,
    @field:JsonProperty("geoplugin_areaCode")
    val areaCode: String?,
    @field:JsonProperty("geoplugin_dmaCode")
    val dmaCode: String?,
    @field:JsonProperty("geoplugin_countryCode")
    val countryCode: String?,
    @field:JsonProperty("geoplugin_countryName")
    val countryName: String?,
    @field:JsonProperty("geoplugin_inEU")
    val inEU: Int,
    @field:JsonProperty("geoplugin_euVATrate")
    val euVATrate: Boolean,
    @field:JsonProperty("geoplugin_continentCode")
    val continentCode: String?,
    @field:JsonProperty("geoplugin_continentName")
    val continentName: String?,
    @field:JsonProperty("geoplugin_latitude")
    val latitude: String?,
    @field:JsonProperty("geoplugin_longitude")
    val longitude: String?,
    @field:JsonProperty("geoplugin_locationAccuracyRadius")
    val locationAccuracyRadius: String?,
    @field:JsonProperty("geoplugin_timezone")
    val timezone: String?,
    @field:JsonProperty("geoplugin_currencyCode")
    val currencyCode: String?,
    @field:JsonProperty("geoplugin_currencySymbol")
    val currencySymbol: String?,
    @field:JsonProperty("geoplugin_currencySymbol_UTF8")
    val currencySymbol_UTF8: String,
    @field:JsonProperty("geoplugin_currencyConverter")
    val currencyConverter: Double,
) {
    fun isSuccess(): Boolean = status == 200
}

@Component
class GeoPluginService {

    @Autowired
    lateinit var restTemplate: RestTemplate

    fun info(ip: String): GeoInfo {
        return restTemplate.getForObject("http://www.geoplugin.net/json.gp?ip=$ip")
    }
}
