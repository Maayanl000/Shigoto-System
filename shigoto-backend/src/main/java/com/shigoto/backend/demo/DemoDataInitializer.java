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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "shigoto.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "ShigotoDemo123!";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Company nvidia = findOrCreateCompany("NVIDIA");
        Company microsoft = findOrCreateCompany("Microsoft");
        Company google = findOrCreateCompany("Google");

        findOrCreateUser("Rachel", "Green", "rachel.green@nvidia.demo", Role.HR, nvidia);
        findOrCreateUser("Monica", "Geller", "monica.geller@nvidia.demo", Role.HR, nvidia);
        findOrCreateUser("Gunther", "", "gunther@nvidia.demo", Role.INTERVIEWER, nvidia);
        findOrCreateUser(
                "Mike", "Hannigan", "mike.hannigan@nvidia.demo", Role.INTERVIEWER, nvidia);
        User chandler = findOrCreateUser(
                "Chandler", "Bing", "chandler.bing@microsoft.demo", Role.INTERVIEWER, microsoft);
        User joey = findOrCreateUser(
                "Joey", "Tribbiani", "joey.tribbiani@microsoft.demo", Role.INTERVIEWER, microsoft);
        findOrCreateUser("Janice", "Litman", "janice.litman@microsoft.demo", Role.HR, microsoft);
        findOrCreateUser("Ross", "Geller", "ross.geller@google.demo", Role.HR, google);
        findOrCreateUser(
                "Phoebe", "Buffay", "phoebe.buffay@google.demo", Role.INTERVIEWER, google);

        User eren = findOrCreateCandidate("Eren", "Yeager", "eren.yeager@candidate.demo");
        User mikasa = findOrCreateCandidate("Mikasa", "Ackerman", "mikasa.ackerman@candidate.demo");
        User armin = findOrCreateCandidate("Armin", "Arlert", "armin.arlert@candidate.demo");
        User levi = findOrCreateCandidate("Levi", "Ackerman", "levi.ackerman@candidate.demo");
        User hange = findOrCreateCandidate("Hange", "Zoe", "hange.zoe@candidate.demo");

        Job nvidiaBackend = findOrCreateJob(nvidia, "Backend Developer",
                "Build reliable Java and Spring services for GPU-powered developer platforms.", "Yokneam");
        Job nvidiaFullStack = findOrCreateJob(nvidia, "Full Stack Developer",
                "Create data-intensive React experiences backed by scalable cloud services.", "Tel Aviv");
        Job microsoftSoftware = findOrCreateJob(microsoft, "Software Engineer",
                "Develop secure distributed services for Microsoft cloud products.", "Herzliya");
        Job microsoftCpp = findOrCreateJob(microsoft, "C++ Developer",
                "Build high-performance systems software with modern C++.", "Haifa");
        Job googleFrontend = findOrCreateJob(google, "Frontend Developer",
                "Build accessible, high-quality web interfaces used at global scale.", "Tel Aviv");
        findOrCreateJob(nvidia, "Student Software Developer",
                "Develop Java and Python tools for GPU developer workflows in a mentored student role.", "Yokneam");
        findOrCreateJob(microsoft, "Java Backend Student",
                "Build and test Java backend services for cloud products alongside experienced engineers.", "Herzliya");
        findOrCreateJob(google, "Software Engineering Student",
                "Contribute to production software and developer tooling as part of a mentored student team.",
                "Tel Aviv");

        LocalDateTime base = LocalDate.now().atTime(9, 0);

        findOrCreateApplication(eren, nvidiaBackend, ApplicationStatus.APPLIED,
                base.minusDays(2), base.minusDays(2), builder -> builder
                        .coverLetter("Interested in dependable backend systems and developer tooling."));
        findOrCreateApplication(mikasa, nvidiaFullStack, ApplicationStatus.HR_INTERVIEW,
                base.minusDays(8), base.minusDays(6), builder -> builder
                        .coverLetter("Experienced across frontend delivery and backend integration."));
        findOrCreateApplication(armin, nvidiaBackend, ApplicationStatus.TASK_SENT,
                base.minusDays(10), base.minusDays(4), builder -> builder
                        .taskInstructions("Design a small API for tracking GPU workload jobs.")
                        .taskDeadline(base.plusDays(3)));
        findOrCreateApplication(hange, nvidiaBackend, ApplicationStatus.REJECTED,
                base.minusDays(14), base.minusDays(3), builder -> builder
                        .candidateFeedback("Strong profile, but another candidate more closely matched the current role."));

        findOrCreateApplication(eren, microsoftSoftware, ApplicationStatus.TASK_SUBMITTED,
                base.minusDays(12), base.minusDays(1), builder -> builder
                        .taskInstructions("Implement a resilient service-health dashboard API.")
                        .taskDeadline(base.plusDays(1))
                        .taskRepoUrl("https://github.com/shigoto-demo/eren-service-health"));
        Application managerInterviewApplication = findOrCreateApplication(
                mikasa, microsoftCpp, ApplicationStatus.TECH_INTERVIEW_SCHEDULED,
                base.minusDays(20), base.minusDays(2), builder -> builder
                        .taskInstructions("Implement and benchmark a thread-safe in-memory cache.")
                        .taskDeadline(base.minusDays(7))
                        .taskRepoUrl("https://github.com/shigoto-demo/mikasa-cpp-cache")
                        .taskReviewNotes("Approved for technical and manager interviews."));

        findOrCreateApplication(armin, googleFrontend, ApplicationStatus.OFFER,
                base.minusDays(18), base.minusDays(1), builder -> builder
                        .candidateFeedback("Offer approved after a strong frontend exercise and interviews."));
        findOrCreateApplication(levi, googleFrontend, ApplicationStatus.APPLIED,
                base.minusDays(1), base.minusDays(1), builder -> builder
                        .coverLetter("Focused on fast, accessible, and maintainable user interfaces."));

        findOrCreateInterview(managerInterviewApplication, chandler, InterviewType.TECHNICAL,
                InterviewStatus.COMPLETED, base.minusDays(3).withHour(14),
                "https://meet.google.com/shigoto-demo-technical",
                "Strong technical performance and clear discussion of concurrency tradeoffs.");
        findOrCreateInterview(managerInterviewApplication, joey, InterviewType.MANAGER,
                InterviewStatus.SCHEDULED, base.plusDays(2).withHour(11),
                "https://meet.google.com/shigoto-demo-manager", null);

        log.info("Deterministic Shigoto demo data is ready");
    }

    private Company findOrCreateCompany(String name) {
        return companyRepository.findByName(name)
                .orElseGet(() -> companyRepository.save(Company.builder().name(name).build()));
    }

    private User findOrCreateCandidate(String firstName, String lastName, String email) {
        String githubProfileUrl = "https://github.com/shigoto-demo-" + firstName.toLowerCase();
        return userRepository.findByEmail(email)
                .map(existing -> {
                    User candidate = validateDemoUser(existing, firstName, lastName, Role.CANDIDATE, null);
                    if (candidate.getGithubProfileUrl() == null) {
                        candidate.setGithubProfileUrl(githubProfileUrl);
                        return userRepository.save(candidate);
                    }
                    return candidate;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .password(passwordEncoder.encode(DEMO_PASSWORD))
                        .role(Role.CANDIDATE)
                        .githubProfileUrl(githubProfileUrl)
                        .build()));
    }

    private User findOrCreateUser(
            String firstName, String lastName, String email, Role role, Company company) {
        return userRepository.findByEmail(email)
                .map(existing -> validateDemoUser(existing, firstName, lastName, role, company))
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .password(passwordEncoder.encode(DEMO_PASSWORD))
                        .role(role)
                        .company(company)
                        .build()));
    }

    private User validateDemoUser(
            User user, String firstName, String lastName, Role role, Company expectedCompany) {
        boolean expectedIdentity = firstName.equals(user.getFirstName())
                && lastName.equals(user.getLastName())
                && role == user.getRole();
        boolean expectedRelationship = expectedCompany == null
                ? user.getCompany() == null
                : user.getCompany() != null
                && Objects.equals(expectedCompany.getId(), user.getCompany().getId());
        if (!expectedIdentity || !expectedRelationship) {
            throw new IllegalStateException("Demo user " + user.getEmail()
                    + " already exists with a different identity, role, or company");
        }
        if (!passwordEncoder.matches(DEMO_PASSWORD, user.getPassword())) {
            throw new IllegalStateException("Demo user " + user.getEmail()
                    + " already exists with a different password");
        }
        return user;
    }

    private Job findOrCreateJob(Company company, String title, String description, String location) {
        return jobRepository.findByCompanyAndTitle(company, title)
                .orElseGet(() -> jobRepository.save(Job.builder()
                        .title(title)
                        .description(description)
                        .location(location)
                        .status(JobStatus.OPEN)
                        .company(company)
                        .build()));
    }

    private Application findOrCreateApplication(
            User candidate,
            Job job,
            ApplicationStatus status,
            LocalDateTime appliedAt,
            LocalDateTime statusChangedAt,
            Consumer<Application.ApplicationBuilder> customize) {
        return applicationRepository.findByCandidateIdAndJobId(candidate.getId(), job.getId())
                .orElseGet(() -> {
                    Application.ApplicationBuilder builder = Application.builder()
                            .candidate(candidate)
                            .job(job)
                            .status(status)
                            .appliedAt(appliedAt)
                            .statusChangedAt(statusChangedAt);
                    customize.accept(builder);
                    return applicationRepository.save(builder.build());
                });
    }

    private Interview findOrCreateInterview(
            Application application,
            User interviewer,
            InterviewType type,
            InterviewStatus status,
            LocalDateTime scheduledAt,
            String meetingLink,
            String feedback) {
        return interviewRepository.findFirstByApplicationIdAndTypeOrderByIdAsc(application.getId(), type)
                .orElseGet(() -> interviewRepository.save(Interview.builder()
                        .application(application)
                        .interviewer(interviewer)
                        .type(type)
                        .status(status)
                        .scheduledAt(scheduledAt)
                        .meetingLink(meetingLink)
                        .feedback(feedback)
                        .build()));
    }
}
