import * as ct from "countries-and-timezones";
import { DateTime } from "luxon";

interface TimeZoneOption {
  label: string;
  value: string;
  offset: number;
  utc: string;
}

const buildTimezoneOptions = (): TimeZoneOption[] => {
  const zones = Object.values(ct.getAllTimezones()) as Array<{
    name: string;
    countries: string[];
    utcOffset: number;
    utcOffsetStr: string;
    dstOffset: number;
    dstOffsetStr: string;
    aliasOf: string | null;
  }>;

  const options = zones
    .map((tz) => {
      const countryCode = tz.countries[0];
      const country = countryCode ? ct.getCountry(countryCode) : null;

      const dateTime = DateTime.now().setZone(tz.name);
      if (!dateTime.isValid) return null;

      const offset = dateTime.toFormat("ZZ");
      const offsetMinutes = dateTime.offset;
      const city = tz.name.split("/").pop()?.replace(/_/g, " ") || tz.name;

      // Concatenating city and country in label for searchability in CustomSelect
      const countryName = country?.name ? ` · ${country.name}` : "";

      return {
        value: tz.name,
        label: `(UTC${offset}) ${city}${countryName}`,
        offset: offsetMinutes,
        utc: `UTC${offset}`,
      };
    })
    .filter((opt): opt is TimeZoneOption => opt !== null);

  // Sort by offset primarily, then alphabetically by label
  return options.sort((a, b) => {
    if (a.offset !== b.offset) return a.offset - b.offset;
    return a.label.localeCompare(b.label);
  });
};

export const TIME_ZONE_OPTIONS = buildTimezoneOptions();
