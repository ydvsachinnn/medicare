package pep.com.pepclass.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pep.com.pepclass.model.PasswordResetOtp;

@Repository
public interface PasswordResetOtpRepository extends MongoRepository<PasswordResetOtp, String> {
    List<PasswordResetOtp> findByEmail(String email);
    Optional<PasswordResetOtp> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
    Optional<PasswordResetOtp> findByEmailAndResetTokenAndUsedFalse(String email, String resetToken);
}
