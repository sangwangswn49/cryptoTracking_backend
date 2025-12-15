package com.example.crypto_backend.dto.response;

import com.example.crypto_backend.entity.Asset;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Data
@AllArgsConstructor
public class UserResponse {
    @NotBlank(message = "Username cannot be blank")
    private String userName;

    private String name;
    private String role;
    private List<String> coinList;
    private List<Asset> assets;
}
