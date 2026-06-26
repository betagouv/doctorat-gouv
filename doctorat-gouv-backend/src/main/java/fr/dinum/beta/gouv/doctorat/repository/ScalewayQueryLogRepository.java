package fr.dinum.beta.gouv.doctorat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.ScalewayQueryLog;

@Repository
public interface ScalewayQueryLogRepository extends JpaRepository<ScalewayQueryLog, Long> {
}
