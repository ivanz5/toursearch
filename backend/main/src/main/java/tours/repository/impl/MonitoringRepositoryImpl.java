package tours.repository.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tours.model.Monitoring;
import tours.repository.contract.MonitoringRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Repository
public class MonitoringRepositoryImpl implements MonitoringRepository {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRepositoryImpl.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH:mm:ss");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Monitoring> getAllActiveMonitorings() {
        Query query = entityManager.createNativeQuery("SELECT * FROM monitorings WHERE active<>0", Monitoring.class);
        return query.getResultList();
    }

    @Override
    public List<Monitoring> getMonitoringsByUser(int userId) {
        Query query = entityManager.createNativeQuery("SELECT * FROM monitorings WHERE user_id=:u", Monitoring.class);
        query.setParameter("u", userId);
        return query.getResultList();
    }

    @Transactional
    @Override
    public int createMonitoring(Monitoring monitoring) {
        String q = "INSERT INTO `monitorings` (`user_id`, `active`, `interval`, `expires`) VALUES (:uid, 1, :interval, :exp)";
        Query query = entityManager.createNativeQuery(q, Monitoring.class);
        query.setParameter("uid", monitoring.getUserId());
        query.setParameter("interval", monitoring.getInterval());
        query.setParameter("exp", monitoring.getExpires());
        query.executeUpdate();
        // Get inserted id
        q = "SELECT * from `monitorings` WHERE user_id = :uid ORDER BY id DESC LIMIT 1";
        Query idQuery = entityManager.createNativeQuery(q, Monitoring.class);
        idQuery.setParameter("uid", monitoring.getUserId());
        List r = idQuery.getResultList();
        if (r != null && r.size() > 0) return ((Monitoring) r.get(0)).getId();
        return 0;
    }

    @Transactional
    @Override
    public void updateMonitoringUpdateTime(Monitoring monitoring) {
        if (monitoring.getNextUpdate() == null) monitoring.setNextUpdate(new Date());
        monitoring.getNextUpdate().setTime(System.currentTimeMillis() - 1000);
        if (monitoring.getNextUpdate().before(new Date())) {
            long millis = monitoring.getInterval() * 1000 + monitoring.getNextUpdate().getTime();
            monitoring.getNextUpdate().setTime(millis);
            Query query = entityManager.createNativeQuery("UPDATE monitorings SET `next_update`=:n WHERE id=:id", Monitoring.class);
            query.setParameter("id", monitoring.getId());
            query.setParameter("n", monitoring.getNextUpdate());
            query.executeUpdate();
            log.info("Updating monitoring next update time: monitoring_id = " + monitoring.getId() +
                    ", time: "+ dateFormat.format(monitoring.getNextUpdate()));
        }
    }

    @Override
    public void setMonitoringActive(int monitoringId, boolean setActive) {

    }
}