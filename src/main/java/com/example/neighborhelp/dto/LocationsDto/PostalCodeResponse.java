
package com.example.neighborhelp.dto.LocationsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostalCodeResponse {

    private String postalCode;     // Postal code
    private String city;           // Associated city
    private Long resourceCount;    // Number of resources with this postal code
}
