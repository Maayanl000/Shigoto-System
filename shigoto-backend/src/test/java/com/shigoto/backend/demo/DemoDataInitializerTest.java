package com.shigoto.backend.demo;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.CompanyRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoDataInitializerTest {

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<String, Company> companies = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Job> jobs = new LinkedHashMap<>();
    private final Map<String, Application> applications = new LinkedHashMap<>();
    private final Map<String, Interview> interviews = new LinkedHashMap<>();

    private CompanyRepository companyRepository;
    private UserRepository userRepository;
    private JobRepository jobRepository;
    private ApplicationRepository applicationRepository;
    private InterviewRepository interviewRepository;
    private PasswordEncoder passwordEncoder;
    private DemoDataInitializer initializer;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        userRepository = mock(UserRepository.class);
        jobRepository = mock(JobRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        interviewRepository = mock(InterviewRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        when(passwordEncoder.encode(DemoDataInitializer.DEMO_PASSWORD)).thenReturn("encoded-demo-password");
        when(passwordEncoder.matches(DemoDataInitializer.DEMO_PASSWORD, "encoded-demo-password"))
                .thenReturn(true);

        configureCompanyRepository();
        configureUserRepository();
        configureJobRepository();
        configureApplicationRepository();
        configureInterviewRepository();

        initializer = new DemoDataInitializer(companyRepository, userRepository, jobRepository,
                applicationRepository, interviewRepository, passwordEncoder);
    }

    @Test
    void propertyGateIsDisabledUnlessExplicitlyEnabled() {
        ConditionalOnProperty condition = DemoDataInitializer.class.getAnnotation(ConditionalOnProperty.class);

        assertNotNull(condition);
        assertArrayEquals(new String[]{"shigoto.demo-data.enabled"}, condition.name());
        assertEquals("true", condition.havingValue());
        assertFalse(condition.matchIfMissing());
    }

    @Test
    void createsDeterministicDatasetAndDoesNotDuplicateItOnSecondRun() throws Exception {
        initializer.run();
        Map<String, ApplicationTimestamps> originalApplicationTimestamps = applicationTimestamps();
        Map<String, LocalDateTime> originalInterviewTimes = interviewTimes();

        initializer.run();

        assertEquals(3, companies.size());
        assertEquals(14, users.size());
        assertEquals(8, jobs.size());
        assertEquals(8, applications.size());
        assertEquals(2, interviews.size());

        assertEquals(Role.HR, users.get("rachel.green@nvidia.demo").getRole());
        assertEquals("NVIDIA", users.get("rachel.green@nvidia.demo").getCompany().getName());
        assertEquals(Role.HR, users.get("monica.geller@nvidia.demo").getRole());
        assertEquals(Role.INTERVIEWER, users.get("gunther@nvidia.demo").getRole());
        assertEquals("Gunther", users.get("gunther@nvidia.demo").getFirstName());
        assertEquals("", users.get("gunther@nvidia.demo").getLastName());
        assertEquals("NVIDIA", users.get("gunther@nvidia.demo").getCompany().getName());
        assertEquals("encoded-demo-password", users.get("gunther@nvidia.demo").getPassword());
        assertEquals(Role.INTERVIEWER, users.get("mike.hannigan@nvidia.demo").getRole());
        assertEquals("Mike", users.get("mike.hannigan@nvidia.demo").getFirstName());
        assertEquals("Hannigan", users.get("mike.hannigan@nvidia.demo").getLastName());
        assertEquals("NVIDIA", users.get("mike.hannigan@nvidia.demo").getCompany().getName());
        assertEquals("encoded-demo-password", users.get("mike.hannigan@nvidia.demo").getPassword());
        assertEquals(Role.INTERVIEWER, users.get("chandler.bing@microsoft.demo").getRole());
        assertEquals(Role.INTERVIEWER, users.get("joey.tribbiani@microsoft.demo").getRole());
        assertEquals(Role.HR, users.get("janice.litman@microsoft.demo").getRole());
        assertEquals(Role.HR, users.get("ross.geller@google.demo").getRole());
        assertEquals(Role.INTERVIEWER, users.get("phoebe.buffay@google.demo").getRole());
        assertNull(users.get("eren.yeager@candidate.demo").getCompany());
        assertEquals(2, countUsers(Role.HR, "NVIDIA"));
        assertEquals(2, countUsers(Role.INTERVIEWER, "NVIDIA"));
        assertEquals(1, countUsers(Role.HR, "Microsoft"));
        assertEquals(2, countUsers(Role.INTERVIEWER, "Microsoft"));
        assertEquals(1, countUsers(Role.HR, "Google"));
        assertEquals(1, countUsers(Role.INTERVIEWER, "Google"));
        assertEquals(5, users.values().stream().filter(user -> user.getRole() == Role.CANDIDATE).count());
        assertEquals("Yokneam", jobs.get("NVIDIA|Student Software Developer").getLocation());
        assertEquals("Herzliya", jobs.get("Microsoft|Java Backend Student").getLocation());
        assertEquals("Tel Aviv", jobs.get("Google|Software Engineering Student").getLocation());
        assertTrue(jobs.values().stream().allMatch(job -> job.getStatus() == JobStatus.OPEN));

        assertTrue(applications.values().stream()
                .map(Application::getStatus)
                .anyMatch(ApplicationStatus.APPLIED::equals));
        assertTrue(applications.values().stream()
                .map(Application::getStatus)
                .anyMatch(ApplicationStatus.HR_INTERVIEW::equals));
        assertTrue(applications.values().stream()
                .map(Application::getStatus)
                .anyMatch(ApplicationStatus.TASK_SENT::equals));
        assertTrue(applications.values().stream()
                .map(Application::getStatus)
                .anyMatch(ApplicationStatus.TECH_INTERVIEW_SCHEDULED::equals));
        assertTrue(applications.values().stream()
                .map(Application::getStatus)
                .anyMatch(ApplicationStatus.REJECTED::equals));
        assertTrue(applications.values().stream()
                .allMatch(application -> application.getStatusChangedAt() != null));
        assertTrue(applications.values().stream()
                .filter(application -> application.getStatus() == ApplicationStatus.APPLIED)
                .allMatch(application -> application.getAppliedAt().equals(application.getStatusChangedAt())));
        assertTrue(applications.values().stream()
                .filter(application -> application.getStatus() != ApplicationStatus.APPLIED)
                .allMatch(application -> application.getStatusChangedAt().isAfter(application.getAppliedAt())));
        assertEquals(originalApplicationTimestamps, applicationTimestamps());
        assertEquals(originalInterviewTimes, interviewTimes());

        Interview technical = interviews.values().stream()
                .filter(interview -> interview.getType() == InterviewType.TECHNICAL)
                .findFirst().orElseThrow();
        Interview manager = interviews.values().stream()
                .filter(interview -> interview.getType() == InterviewType.MANAGER)
                .findFirst().orElseThrow();
        assertEquals(InterviewStatus.COMPLETED, technical.getStatus());
        assertEquals("chandler.bing@microsoft.demo", technical.getInterviewer().getEmail());
        assertTrue(technical.getScheduledAt().isBefore(LocalDateTime.now()));
        assertEquals(InterviewStatus.SCHEDULED, manager.getStatus());
        assertEquals("joey.tribbiani@microsoft.demo", manager.getInterviewer().getEmail());
        assertTrue(manager.getScheduledAt().isAfter(LocalDateTime.now()));
        assertTrue(manager.getScheduledAt().isAfter(technical.getScheduledAt()));

        verify(companyRepository, times(3)).save(any(Company.class));
        verify(userRepository, times(14)).save(any(User.class));
        verify(passwordEncoder, times(14)).encode(DemoDataInitializer.DEMO_PASSWORD);
        verify(jobRepository, times(8)).save(any(Job.class));
        verify(applicationRepository, times(8)).save(any(Application.class));
        verify(interviewRepository, times(2)).save(any(Interview.class));
    }

    private Map<String, ApplicationTimestamps> applicationTimestamps() {
        Map<String, ApplicationTimestamps> timestamps = new LinkedHashMap<>();
        applications.forEach((key, application) -> timestamps.put(key,
                new ApplicationTimestamps(application.getAppliedAt(), application.getStatusChangedAt())));
        return timestamps;
    }

    private Map<String, LocalDateTime> interviewTimes() {
        Map<String, LocalDateTime> timestamps = new LinkedHashMap<>();
        interviews.forEach((key, interview) -> timestamps.put(key, interview.getScheduledAt()));
        return timestamps;
    }

    private long countUsers(Role role, String companyName) {
        return users.values().stream()
                .filter(user -> user.getRole() == role)
                .filter(user -> user.getCompany() != null)
                .filter(user -> companyName.equals(user.getCompany().getName()))
                .count();
    }

    private void configureCompanyRepository() {
        when(companyRepository.findByName(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(companies.get(invocation.getArgument(0))));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            assignId(company);
            companies.put(company.getName(), company);
            return company;
        });
    }

    private void configureUserRepository() {
        when(userRepository.findByEmail(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(users.get(invocation.getArgument(0))));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assignId(user);
            users.put(user.getEmail(), user);
            return user;
        });
    }

    private void configureJobRepository() {
        when(jobRepository.findByCompanyAndTitle(any(Company.class), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(jobs.get(jobKey(
                        invocation.getArgument(0), invocation.getArgument(1)))));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            assignId(job);
            jobs.put(jobKey(job.getCompany(), job.getTitle()), job);
            return job;
        });
    }

    private void configureApplicationRepository() {
        when(applicationRepository.findByCandidateIdAndJobId(any(Long.class), any(Long.class)))
                .thenAnswer(invocation -> Optional.ofNullable(applications.get(
                        applicationKey(invocation.getArgument(0), invocation.getArgument(1)))));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            assignId(application);
            applications.put(applicationKey(application.getCandidate().getId(), application.getJob().getId()),
                    application);
            return application;
        });
    }

    private void configureInterviewRepository() {
        when(interviewRepository.findFirstByApplicationIdAndTypeOrderByIdAsc(
                any(Long.class), any(InterviewType.class)))
                .thenAnswer(invocation -> Optional.ofNullable(interviews.get(
                        interviewKey(invocation.getArgument(0), invocation.getArgument(1)))));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> {
            Interview interview = invocation.getArgument(0);
            assignId(interview);
            interviews.put(interviewKey(interview.getApplication().getId(), interview.getType()), interview);
            return interview;
        });
    }

    private String jobKey(Company company, String title) {
        return company.getName() + "|" + title;
    }

    private String applicationKey(Long candidateId, Long jobId) {
        return candidateId + "|" + jobId;
    }

    private String interviewKey(Long applicationId, InterviewType type) {
        return applicationId + "|" + type;
    }

    private void assignId(Object entity) {
        long id = ids.getAndIncrement();
        if (entity instanceof Company company && company.getId() == null) company.setId(id);
        if (entity instanceof User user && user.getId() == null) user.setId(id);
        if (entity instanceof Job job && job.getId() == null) job.setId(id);
        if (entity instanceof Application application && application.getId() == null) application.setId(id);
        if (entity instanceof Interview interview && interview.getId() == null) interview.setId(id);
    }

    private record ApplicationTimestamps(LocalDateTime appliedAt, LocalDateTime statusChangedAt) {}
}
