package org.example.lab9;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class Item {
    @NotNull(message = "У товара должно быть имя")
    private String name;
    private String characteristics;
    private UUID itemId;

    @NotNull(message = "Цена не может быть null")
    @DecimalMin(value = "0.01", inclusive = false, message = "Цена должна быть больше 0")
    private BigDecimal price;
}
