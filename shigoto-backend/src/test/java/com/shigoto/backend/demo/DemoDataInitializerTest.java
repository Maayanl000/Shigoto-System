package com.shigoto.backend.demo;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.EmploymentType;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(13, users.size());
        assertEquals(8, jobs.size());
        assertEquals(8, applications.size());
        assertEquals(3, interviews.size());

        assertEquals(Set.of("NVIDIA", "Microsoft", "Google"), companies.keySet());
        assertEquals(Set.of(
                "rachel.green@nvidia.demo", "gunther@nvidia.demo", "mike.hannigan@nvidia.demo",
                "janice.litman@microsoft.demo", "chandler.bing@microsoft.demo",
                "joey.tribbiani@microsoft.demo", "ross.geller@google.demo", "phoebe.buffay@google.demo",
                "eren.yeager@candidate.demo", "mikasa.ackerman@candidate.demo",
                "armin.arlert@candidate.demo", "levi.ackerman@candidate.demo", "hange.zoe@candidate.demo"
        ), users.keySet());

        assertEquals(Role.HR, users.get("rachel.green@nvidia.demo").getRole());
        assertEquals("NVIDIA", users.get("rachel.green@nvidia.demo").getCompany().getName());
        assertFalse(users.containsKey("monica.geller@nvidia.demo"));
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
        assertEquals(1, countUsers(Role.HR, "NVIDIA"));
        assertEquals(2, countUsers(Role.INTERVIEWER, "NVIDIA"));
        assertEquals(1, countUsers(Role.HR, "Microsoft"));
        assertEquals(2, countUsers(Role.INTERVIEWER, "Microsoft"));
        assertEquals(1, countUsers(Role.HR, "Google"));
        assertEquals(1, countUsers(Role.INTERVIEWER, "Google"));
        assertEquals(5, users.values().stream().filter(user -> user.getRole() == Role.CANDIDATE).count());
        assertCandidateProfile("eren.yeager@candidate.demo", "Computer Science Student", "Backend Developer",
                EmploymentType.STUDENT, true, "https://github.com/vladmihalcea");
        assertCandidateProfile("mikasa.ackerman@candidate.demo", "Full Stack Developer", "Software Engineer",
                EmploymentType.FULL_TIME, false, "https://github.com/gaearon");
        assertCandidateProfile("armin.arlert@candidate.demo", "Frontend Developer", "Frontend Engineer",
                EmploymentType.INTERNSHIP, true, "https://github.com/sindresorhus");
        assertCandidateProfile("levi.ackerman@candidate.demo", "Systems Programmer", "C++ Developer",
                EmploymentType.FULL_TIME, false, "https://github.com/torvalds");
        assertCandidateProfile("hange.zoe@candidate.demo", "Software Engineering Student",
                "Backend Engineering Intern", EmploymentType.INTERNSHIP, true,
                "https://github.com/gvanrossum");
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
        assertEquals(2, countApplications(ApplicationStatus.APPLIED));
        assertEquals(1, countApplications(ApplicationStatus.HR_INTERVIEW));
        assertEquals(1, countApplications(ApplicationStatus.TASK_SENT));
        assertEquals(1, countApplications(ApplicationStatus.TASK_SUBMITTED));
        assertEquals(1, countApplications(ApplicationStatus.TECH_INTERVIEW_SCHEDULED));
        assertEquals(1, countApplications(ApplicationStatus.OFFER));
        assertEquals(1, countApplications(ApplicationStatus.REJECTED));
        assertTrue(applications.values().stream()
                .allMatch(application -> application.getStatusChangedAt() != null));
        assertTrue(applications.values().stream()
                .filter(application -> application.getStatus() == ApplicationStatus.APPLIED)
                .allMatch(application -> application.getAppliedAt().equals(application.getStatusChangedAt())));
        assertTrue(applications.values().stream()
                .filter(application -> application.getStatus() != ApplicationStatus.APPLIED)
                .allMatch(application -> application.getStatusChangedAt().isAfter(application.getAppliedAt())));
        assertCandidateApplicationsUseCv("eren.yeager@candidate.demo", DemoDataInitializer.EREN_CV_STORAGE_KEY);
        assertCandidateApplicationsUseCv("mikasa.ackerman@candidate.demo", DemoDataInitializer.MIKASA_CV_STORAGE_KEY);
        assertCandidateApplicationsUseCv("armin.arlert@candidate.demo", DemoDataInitializer.ARMIN_CV_STORAGE_KEY);
        assertNull(application("hange.zoe@candidate.demo", "NVIDIA", "Backend Developer").getCvUrl());
        assertNull(application("levi.ackerman@candidate.demo", "Google", "Frontend Developer").getCvUrl());
        assertEquals(3, applications.values().stream()
                .filter(application -> application.getCoverLetter() != null
                        && !application.getCoverLetter().isBlank())
                .count());
        assertTrue(application("eren.yeager@candidate.demo", "NVIDIA", "Backend Developer")
                .getCoverLetter().contains("Java development, API design, and database work"));
        assertTrue(application("mikasa.ackerman@candidate.demo", "NVIDIA", "Full Stack Developer")
                .getCoverLetter().contains("React, JavaScript, C++"));
        assertTrue(application("armin.arlert@candidate.demo", "Google", "Frontend Developer")
                .getCoverLetter().contains("responsive UI development"));
        assertNull(application("levi.ackerman@candidate.demo", "Google", "Frontend Developer")
                .getCoverLetter());
        assertEquals("https://github.com/spring-guides/gs-rest-service",
                application("eren.yeager@candidate.demo", "Microsoft", "Software Engineer").getTaskRepoUrl());
        assertEquals("https://github.com/facebook/folly",
                application("mikasa.ackerman@candidate.demo", "Microsoft", "C++ Developer").getTaskRepoUrl());
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
        verify(userRepository, times(13)).save(any(User.class));
        verify(passwordEncoder, times(13)).encode(DemoDataInitializer.DEMO_PASSWORD);
        verify(jobRepository, times(8)).save(any(Job.class));
        verify(applicationRepository, times(8)).save(any(Application.class));
        Interview offerTechnical = interview(
                "armin.arlert@candidate.demo", "Google", "Frontend Developer", InterviewType.TECHNICAL);
        assertEquals(InterviewStatus.COMPLETED, offerTechnical.getStatus());
        assertEquals("phoebe.buffay@google.demo", offerTechnical.getInterviewer().getEmail());
        assertTrue(offerTechnical.getFeedback().contains("accessibility awareness"));

        verify(interviewRepository, times(3)).save(any(Interview.class));
    }

    @Test
    void rejectsExistingCompanyWithMultipleHrUsersWithoutDeletingEither() {
        Company nvidia = Company.builder().id(100L).name("NVIDIA").build();
        companies.put(nvidia.getName(), nvidia);
        users.put("rachel.green@nvidia.demo", User.builder().id(101L).firstName("Rachel")
                .lastName("Green").email("rachel.green@nvidia.demo").password("encoded-demo-password")
                .role(Role.HR).company(nvidia).build());
        users.put("monica.geller@nvidia.demo", User.builder().id(102L).firstName("Monica")
                .lastName("Geller").email("monica.geller@nvidia.demo").password("encoded-demo-password")
                .role(Role.HR).company(nvidia).build());

        IllegalStateException failure = assertThrows(IllegalStateException.class, initializer::run);

        assertTrue(failure.getMessage().contains("manual cleanup is required"));
        assertEquals(2, countUsers(Role.HR, "NVIDIA"));
    }

    @Test
    void preservesAnExistingSoleCompanyHrInsteadOfCreatingThePreferredSeed() throws Exception {
        Company nvidia = Company.builder().id(100L).name("NVIDIA").build();
        companies.put(nvidia.getName(), nvidia);
        users.put("monica.geller@nvidia.demo", User.builder().id(102L).firstName("Monica")
                .lastName("Geller").email("monica.geller@nvidia.demo").password("existing-password")
                .role(Role.HR).company(nvidia).build());

        initializer.run();

        assertTrue(users.containsKey("monica.geller@nvidia.demo"));
        assertFalse(users.containsKey("rachel.green@nvidia.demo"));
        assertEquals(1, countUsers(Role.HR, "NVIDIA"));
        assertEquals(2, countUsers(Role.INTERVIEWER, "NVIDIA"));
    }

    @Test
    void fillsMissingDemoCvWithoutOverwritingExistingCustomCv() throws Exception {
        initializer.run();
        Application missingCv = application(
                "eren.yeager@candidate.demo", "NVIDIA", "Backend Developer");
        Application customCv = application(
                "mikasa.ackerman@candidate.demo", "Microsoft", "C++ Developer");
        missingCv.setCvUrl(null);
        customCv.setCvUrl("123e4567-e89b-12d3-a456-426614174000.pdf");

        initializer.run();

        assertEquals(DemoDataInitializer.EREN_CV_STORAGE_KEY, missingCv.getCvUrl());
        assertEquals("123e4567-e89b-12d3-a456-426614174000.pdf", customCv.getCvUrl());
        verify(applicationRepository, times(9)).save(any(Application.class));
    }

    @Test
    void repairsLegacySparseCandidateProfilesWithoutOverwritingCustomValues() throws Exception {
        initializer.run();
        User eren = users.get("eren.yeager@candidate.demo");
        eren.setGithubProfileUrl("https://github.com/shigoto-demo-eren");
        eren.setCurrentTitle(null);
        eren.setDesiredRole(null);
        eren.setEmploymentType(null);
        eren.setStudent(false);
        User mikasa = users.get("mikasa.ackerman@candidate.demo");
        mikasa.setGithubProfileUrl("https://github.com/custom-profile");
        mikasa.setCurrentTitle("Custom title");
        mikasa.setDesiredRole(null);
        mikasa.setEmploymentType(null);

        initializer.run();

        assertCandidateProfile("eren.yeager@candidate.demo", "Computer Science Student", "Backend Developer",
                EmploymentType.STUDENT, true, "https://github.com/vladmihalcea");
        assertEquals("https://github.com/custom-profile", mikasa.getGithubProfileUrl());
        assertEquals("Custom title", mikasa.getCurrentTitle());
        assertEquals("Software Engineer", mikasa.getDesiredRole());
        assertEquals(EmploymentType.FULL_TIME, mikasa.getEmploymentType());
    }

    @Test
    void fillsMissingIntendedCoverLetterWithoutOverwritingCustomLetter() throws Exception {
        initializer.run();
        Application eren = application("eren.yeager@candidate.demo", "NVIDIA", "Backend Developer");
        Application mikasa = application("mikasa.ackerman@candidate.demo", "NVIDIA", "Full Stack Developer");
        eren.setCoverLetter(null);
        mikasa.setCoverLetter("Custom candidate-authored letter");

        initializer.run();

        assertTrue(eren.getCoverLetter().contains("clean architecture, validation, and maintainability"));
        assertEquals("Custom candidate-authored letter", mikasa.getCoverLetter());
    }

    @Test
    void repairsLegacyTaskRepositoryWithoutOverwritingCustomRepository() throws Exception {
        initializer.run();
        Application eren = application("eren.yeager@candidate.demo", "Microsoft", "Software Engineer");
        Application mikasa = application("mikasa.ackerman@candidate.demo", "Microsoft", "C++ Developer");
        eren.setTaskRepoUrl("https://github.com/shigoto-demo/eren-service-health");
        mikasa.setTaskRepoUrl("https://github.com/custom/example");

        initializer.run();

        assertEquals("https://github.com/spring-guides/gs-rest-service", eren.getTaskRepoUrl());
        assertEquals("https://github.com/custom/example", mikasa.getTaskRepoUrl());
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

    private long countApplications(ApplicationStatus status) {
        return applications.values().stream()
                .filter(application -> application.getStatus() == status)
                .count();
    }

    private void assertCandidateProfile(
            String email,
            String currentTitle,
            String desiredRole,
            EmploymentType employmentType,
            boolean student,
            String githubProfileUrl) {
        User candidate = users.get(email);
        assertEquals(currentTitle, candidate.getCurrentTitle());
        assertEquals(desiredRole, candidate.getDesiredRole());
        assertEquals(employmentType, candidate.getEmploymentType());
        assertEquals(student, candidate.isStudent());
        assertEquals(githubProfileUrl, candidate.getGithubProfileUrl());
    }

    private void assertCandidateApplicationsUseCv(String candidateEmail, String expectedCvStorageKey) {
        assertTrue(applications.values().stream()
                .filter(application -> candidateEmail.equals(application.getCandidate().getEmail()))
                .allMatch(application -> expectedCvStorageKey.equals(application.getCvUrl())));
    }

    private Application application(String candidateEmail, String companyName, String jobTitle) {
        return applications.values().stream()
                .filter(candidateApplication -> candidateEmail.equals(candidateApplication.getCandidate().getEmail()))
                .filter(candidateApplication -> companyName.equals(candidateApplication.getJob().getCompany().getName()))
                .filter(candidateApplication -> jobTitle.equals(candidateApplication.getJob().getTitle()))
                .findFirst()
                .orElseThrow();
    }

    private Interview interview(
            String candidateEmail, String companyName, String jobTitle, InterviewType type) {
        Long applicationId = application(candidateEmail, companyName, jobTitle).getId();
        return interviews.get(interviewKey(applicationId, type));
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
        when(userRepository.findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(
                any(Role.class), any(Company.class))).thenAnswer(invocation -> {
                    Role role = invocation.getArgument(0);
                    Company company = invocation.getArgument(1);
                    return users.values().stream()
                            .filter(user -> user.getRole() == role)
                            .filter(user -> user.getCompany() != null)
                            .filter(user -> user.getCompany().getId().equals(company.getId()))
                            .sorted(java.util.Comparator.comparing(User::getFirstName)
                                    .thenComparing(User::getLastName))
                            .toList();
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
