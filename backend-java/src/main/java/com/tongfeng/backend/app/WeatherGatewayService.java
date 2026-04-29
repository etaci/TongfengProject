package com.tongfeng.backend.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WeatherGatewayService {

	private final RestClient geocodingClient;
	private final RestClient forecastClient;
	private final AppProperties appProperties;

	public WeatherGatewayService(AppProperties appProperties) {
		this.appProperties = appProperties;
		this.geocodingClient = RestClient.builder()
				.baseUrl(appProperties.getWeatherGeocodingBaseUrl())
				.build();
		this.forecastClient = RestClient.builder()
				.baseUrl(appProperties.getWeatherForecastBaseUrl())
				.build();
	}

	public WeatherObservation queryTodayWeather(String city, String countryCode, LocalDate summaryDate) {
		if (!appProperties.isWeatherLiveEnabled()) {
			return fallbackObservation(city, countryCode, summaryDate);
		}
		try {
			ResolvedLocation location = resolveLocation(city, countryCode);
			if (location == null) {
				return fallbackObservation(city, countryCode, summaryDate);
			}
			ForecastResponse response = forecastClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/v1/forecast")
							.queryParam("latitude", location.latitude())
							.queryParam("longitude", location.longitude())
							.queryParam("forecast_days", 1)
							.queryParam("timezone", "auto")
							.queryParam("hourly", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,weather_code")
							.build())
					.retrieve()
					.body(ForecastResponse.class);
			WeatherObservation observation = mapForecast(location, response, summaryDate);
			return observation == null ? fallbackObservation(city, countryCode, summaryDate) : observation;
		} catch (RestClientException | IllegalArgumentException ex) {
			return fallbackObservation(city, countryCode, summaryDate);
		}
	}

	public ResolvedLocation resolveLocation(String city, String countryCode) {
		if (!appProperties.isWeatherLiveEnabled()) {
			return null;
		}
		try {
			GeocodingResponse response = geocodingClient.get()
					.uri(uriBuilder -> {
						var builder = uriBuilder
								.path("/v1/search")
								.queryParam("name", city)
								.queryParam("count", 1)
								.queryParam("language", "zh");
						if (StringUtils.hasText(countryCode)) {
							builder.queryParam("countryCode", countryCode.toUpperCase(Locale.ROOT));
						}
						return builder.build();
					})
					.retrieve()
					.body(GeocodingResponse.class);
			if (response == null || response.results() == null || response.results().isEmpty()) {
				return null;
			}
			GeocodingResult result = response.results().getFirst();
			String resolvedName = StringUtils.hasText(result.name()) ? result.name() : city;
			return new ResolvedLocation(
					resolvedName,
					result.countryCode(),
					result.latitude(),
					result.longitude(),
					result.timezone()
			);
		} catch (RestClientException ex) {
			return null;
		}
	}

	private WeatherObservation mapForecast(ResolvedLocation location, ForecastResponse response, LocalDate summaryDate) {
		if (response == null || response.hourly() == null || response.hourly().time() == null || response.hourly().time().isEmpty()) {
			return null;
		}
		HourlyWeather hourly = response.hourly();
		ZoneId zoneId = ZoneId.of(StringUtils.hasText(response.timezone()) ? response.timezone() : "Asia/Shanghai");
		LocalDateTime targetHour = LocalDateTime.now(zoneId).truncatedTo(ChronoUnit.HOURS);
		int index = hourly.time().indexOf(targetHour.toString());
		if (index < 0) {
			index = Math.min(hourly.time().size() - 1, LocalDateTime.now(zoneId).getHour());
		}
		return new WeatherObservation(
				location.resolvedName(),
				location.countryCode(),
				location.latitude(),
				location.longitude(),
				StringUtils.hasText(response.timezone()) ? response.timezone() : location.timezoneId(),
				summaryDate,
				numberAt(hourly.temperature2m(), index),
				numberAt(hourly.apparentTemperature(), index),
				intAt(hourly.relativeHumidity2m(), index),
				intAt(hourly.precipitationProbability(), index),
				intAt(hourly.weatherCode(), index),
				"LIVE",
				mapWeatherCodeToText(intAt(hourly.weatherCode(), index))
		);
	}

	private WeatherObservation fallbackObservation(String city, String countryCode, LocalDate summaryDate) {
		int month = summaryDate.getMonthValue();
		int seed = Math.abs(Objects.hash(city, countryCode, summaryDate));
		double seasonalBase = switch (month) {
			case 12, 1, 2 -> 7;
			case 3, 4, 5 -> 19;
			case 6, 7, 8 -> 30;
			default -> 21;
		};
		double temperature = seasonalBase + ((seed % 9) - 4);
		double apparent = temperature + (((seed / 11) % 7) - 3);
		int humidity = 52 + (seed % 35);
		int precipitation = (seed / 17) % 75;
		int weatherCode = precipitation >= 60 ? 61 : (humidity >= 80 ? 3 : 1);
		return new WeatherObservation(
				city,
				countryCode,
				null,
				null,
				"Asia/Shanghai",
				summaryDate,
				scale(temperature),
				scale(apparent),
				humidity,
				precipitation,
				weatherCode,
				"FALLBACK",
				mapWeatherCodeToText(weatherCode)
		);
	}

	private BigDecimal numberAt(List<BigDecimal> values, int index) {
		if (values == null || values.isEmpty()) {
			return BigDecimal.ZERO;
		}
		int safeIndex = Math.max(0, Math.min(index, values.size() - 1));
		return values.get(safeIndex);
	}

	private Integer intAt(List<Integer> values, int index) {
		if (values == null || values.isEmpty()) {
			return 0;
		}
		int safeIndex = Math.max(0, Math.min(index, values.size() - 1));
		return values.get(safeIndex);
	}

	private BigDecimal scale(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
	}

	private String mapWeatherCodeToText(Integer weatherCode) {
		if (weatherCode == null) {
			return "天气数据不足";
		}
		return switch (weatherCode) {
			case 0 -> "晴朗";
			case 1, 2, 3 -> "多云";
			case 45, 48 -> "有雾";
			case 51, 53, 55, 61, 63, 65, 80, 81, 82 -> "降雨";
			case 71, 73, 75, 85, 86 -> "降雪";
			case 95, 96, 99 -> "强对流";
			default -> "天气波动";
		};
	}

	private record GeocodingResponse(List<GeocodingResult> results) {
	}

	private record GeocodingResult(
			String name,
			@JsonProperty("country_code")
			String countryCode,
			Double latitude,
			Double longitude,
			String timezone
	) {
	}

	private record ForecastResponse(String timezone, HourlyWeather hourly) {
	}

	private record HourlyWeather(
			List<String> time,
			@JsonProperty("temperature_2m")
			List<BigDecimal> temperature2m,
			@JsonProperty("apparent_temperature")
			List<BigDecimal> apparentTemperature,
			@JsonProperty("relative_humidity_2m")
			List<Integer> relativeHumidity2m,
			@JsonProperty("precipitation_probability")
			List<Integer> precipitationProbability,
			@JsonProperty("weather_code")
			List<Integer> weatherCode
	) {
	}

	public record ResolvedLocation(
			String resolvedName,
			String countryCode,
			Double latitude,
			Double longitude,
			String timezoneId
	) {
	}

	public record WeatherObservation(
			String cityName,
			String countryCode,
			Double latitude,
			Double longitude,
			String timezoneId,
			LocalDate summaryDate,
			BigDecimal temperatureC,
			BigDecimal apparentTemperatureC,
			Integer relativeHumidity,
			Integer precipitationProbability,
			Integer weatherCode,
			String sourceType,
			String weatherText
	) {
	}
}
