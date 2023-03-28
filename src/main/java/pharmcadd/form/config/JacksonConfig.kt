package pharmcadd.form.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import pharmcadd.form.common.Constants

@Configuration
class JacksonConfig : Jackson2ObjectMapperBuilderCustomizer {

    override fun customize(jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder) {
        jacksonObjectMapperBuilder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        jacksonObjectMapperBuilder.serializers(LocalDateSerializer(Constants.LOCAL_DATE_FORMAT))
        jacksonObjectMapperBuilder.deserializers(LocalDateDeserializer(Constants.LOCAL_DATE_FORMAT))

        jacksonObjectMapperBuilder.serializers(LocalDateTimeSerializer(Constants.LOCAL_DATE_TIME_FORMAT))
        jacksonObjectMapperBuilder.deserializers(LocalDateTimeDeserializer(Constants.LOCAL_DATE_TIME_FORMAT))
    }
}
