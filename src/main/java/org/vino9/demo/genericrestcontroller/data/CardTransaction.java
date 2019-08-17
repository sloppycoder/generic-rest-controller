package org.vino9.demo.genericrestcontroller.data;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class CardTransaction {
    @Id
    Long id;
    double amount;
    String memo;
}
