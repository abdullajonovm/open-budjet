package uz.khodirjob.openbudjet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String firstName;
    private String userName;
    private String phoneNumber;
    private Boolean isVoted;
    @Column(unique = true)
    private Long chatId;
    private String lastRequest;
    private Boolean isAdmin;
    private Boolean isBlocked;

}
