package uz.khodirjob.openbudjet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.khodirjob.openbudjet.entity.User;
import uz.khodirjob.openbudjet.entity.Vote;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {
}
