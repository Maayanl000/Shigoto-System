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

/**
 * Seeds deterministic demo data without replacing existing records.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@Component
@ConditionalOnProperty(name = "shigoto.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "ShigotoDemo123!";
    static final String EREN_CV_STORAGE_KEY = "demo-eren-yeager-cv.pdf";
    static final String MIKASA_CV_STORAGE_KEY = "demo-mikasa-ackerman-cv.pdf";
    static final String ARMIN_CV_STORAGE_KEY = "demo-armin-arlert-cv.pdf";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates or repairs the deterministic demo dataset during application startup.
     * @param args application startup arguments
     */
    @Override
    @Transactional
    public void run(String... args) {
        Company nvidia = findOrCreateCompany("NVIDIA");
        Company microsoft = findOrCreateCompany("Microsoft");
        Company google = findOrCreateCompany("Google");

        findOrCreateCompanyHr("Rachel", "Green", "rachel.green@nvidia.demo", nvidia);
        User gunther = findOrCreateUser("Gunther", "", "gunther@nvidia.demo", Role.INTERVIEWER, nvidia);
        findOrCreateUser(
                "Mike", "Hannigan", "mike.hannigan@nvidia.demo", Role.INTERVIEWER, nvidia);
        User chandler = findOrCreateUser(
                "Chandler", "Bing", "chandler.bing@microsoft.demo", Role.INTERVIEWER, microsoft);
        User joey = findOrCreateUser(
                "Joey", "Tribbiani", "joey.tribbiani@microsoft.demo", Role.INTERVIEWER, microsoft);
        findOrCreateCompanyHr("Janice", "Litman", "janice.litman@microsoft.demo", microsoft);
        findOrCreateCompanyHr("Ross", "Geller", "ross.geller@google.demo", google);
        User phoebe = findOrCreateUser(
                "Phoebe", "Buffay", "phoebe.buffay@google.demo", Role.INTERVIEWER, google);

        // Public profiles are technical fixtures for demonstrating the GitHub API integration only;
        // they do not represent or imply any connection to these fictional candidate identities.
        User eren = findOrCreateCandidate("Eren", "Yeager", "eren.yeager@candidate.demo",
                "Computer Science Student", "Backend Developer", EmploymentType.STUDENT, true,
                "https://github.com/vladmihalcea");
        User mikasa = findOrCreateCandidate("Mikasa", "Ackerman", "mikasa.ackerman@candidate.demo",
                "Full Stack Developer", "Software Engineer", EmploymentType.FULL_TIME, false,
                "https://github.com/gaearon");
        User armin = findOrCreateCandidate("Armin", "Arlert", "armin.arlert@candidate.demo",
                "Frontend Developer", "Frontend Engineer", EmploymentType.INTERNSHIP, true,
                "https://github.com/sindresorhus");
        User levi = findOrCreateCandidate("Levi", "Ackerman", "levi.ackerman@candidate.demo",
                "Systems Programmer", "C++ Developer", EmploymentType.FULL_TIME, false,
                "https://github.com/torvalds");
        User hange = findOrCreateCandidate("Hange", "Zoe", "hange.zoe@candidate.demo",
                "Software Engineering Student", "Backend Engineering Intern", EmploymentType.INTERNSHIP, true,
                "https://github.com/gvanrossum");

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
                base.minusDays(2), base.minusDays(2), EREN_CV_STORAGE_KEY,
                "I am interested in this backend position because it combines Java development, API design, "
                        + "and database work, which are areas I have focused on during my Computer Science studies "
                        + "and personal projects. I enjoy building reliable backend services and working on systems "
                        + "where clean architecture, validation, and maintainability matter.", builder -> {});
        findOrCreateApplication(mikasa, nvidiaFullStack, ApplicationStatus.HR_INTERVIEW,
                base.minusDays(8), base.minusDays(6), MIKASA_CV_STORAGE_KEY,
                "I am applying for this role because it matches my experience with both application development "
                        + "and systems programming. I have worked with React, JavaScript, C++, and backend "
                        + "integrations, and I enjoy projects that require both strong technical foundations and "
                        + "attention to user experience.", builder -> {});
        assignDemoTaskReviewer(findOrCreateApplication(armin, nvidiaBackend, ApplicationStatus.TASK_SENT,
                base.minusDays(10), base.minusDays(4), ARMIN_CV_STORAGE_KEY, null, builder -> builder
                        .taskInstructions("Design a small API for tracking GPU workload jobs.")
                        .taskDeadline(base.plusDays(3))), gunther);
        findOrCreateApplication(hange, nvidiaBackend, ApplicationStatus.REJECTED,
                base.minusDays(14), base.minusDays(3), null, null, builder -> builder
                        .candidateFeedback("Strong profile, but another candidate more closely matched the current role."));

        // Public repositories are technical display fixtures only and do not represent candidate authorship.
        Application submittedTaskApplication = findOrCreateApplication(
                eren, microsoftSoftware, ApplicationStatus.TASK_SUBMITTED,
                base.minusDays(12), base.minusDays(1), EREN_CV_STORAGE_KEY, null, builder -> builder
                        .taskInstructions("Implement a resilient service-health dashboard API.")
                        .taskDeadline(base.plusDays(1))
                        .taskRepoUrl("https://github.com/spring-guides/gs-rest-service"));
        populateDemoTaskRepositoryIfMissingOrLegacy(submittedTaskApplication,
                "https://github.com/shigoto-demo/eren-service-health",
                "https://github.com/spring-guides/gs-rest-service");
        assignDemoTaskReviewer(submittedTaskApplication, chandler);
        Application managerInterviewApplication = findOrCreateApplication(
                mikasa, microsoftCpp, ApplicationStatus.TECH_INTERVIEW_SCHEDULED,
                base.minusDays(20), base.minusDays(2), MIKASA_CV_STORAGE_KEY, null, builder -> builder
                        .taskInstructions("Implement and benchmark a thread-safe in-memory cache.")
                        .taskDeadline(base.minusDays(7))
                        .taskRepoUrl("https://github.com/facebook/folly")
                        .taskReviewNotes("Approved for technical and manager interviews."));
        populateDemoTaskRepositoryIfMissingOrLegacy(managerInterviewApplication,
                "https://github.com/shigoto-demo/mikasa-cpp-cache", "https://github.com/facebook/folly");
        assignDemoTaskReviewer(managerInterviewApplication, chandler);

        Application offeredApplication = findOrCreateApplication(armin, googleFrontend, ApplicationStatus.OFFER,
                base.minusDays(18), base.minusDays(1), ARMIN_CV_STORAGE_KEY,
                "I am interested in this frontend position because I enjoy turning product requirements into "
                        + "clear and usable interfaces. My recent work has focused on React, responsive UI "
                        + "development, form validation, and REST API integration.", builder -> builder
                        .candidateFeedback("Offer approved after a strong frontend exercise and interviews."));
        findOrCreateApplication(levi, googleFrontend, ApplicationStatus.APPLIED,
                base.minusDays(1), base.minusDays(1), null, null, builder -> {});

        findOrCreateInterview(managerInterviewApplication, chandler, InterviewType.TECHNICAL,
                InterviewStatus.COMPLETED, base.minusDays(3).withHour(14),
                "https://meet.google.com/shigoto-demo-technical",
                "Strong technical performance and clear discussion of concurrency tradeoffs.");
        findOrCreateInterview(managerInterviewApplication, joey, InterviewType.MANAGER,
                InterviewStatus.SCHEDULED, base.plusDays(2).withHour(11),
                "https://meet.google.com/shigoto-demo-manager", null);
        findOrCreateInterview(offeredApplication, phoebe, InterviewType.TECHNICAL,
                InterviewStatus.COMPLETED, base.minusDays(2).withHour(10),
                "https://meet.google.com/shigoto-demo-frontend",
                "Excellent frontend fundamentals, accessibility awareness, and API integration decisions.");

        log.info("Deterministic Shigoto demo data is ready");
    }

    /**
     * Reuses the named demo company or persists it when absent.
     * @param name the name to look up
     * @return the existing company with the supplied name, or the newly persisted company
     */
    private Company findOrCreateCompany(String name) {
        return companyRepository.findByName(name)
                .orElseGet(() -> companyRepository.save(Company.builder().name(name).build()));
    }

    /**
     * Reuses and validates the candidate identified by email, or persists the configured demo candidate.
     * @param firstName the first name
     * @param lastName the last name
     * @param email the email address
     * @param currentTitle the current title
     * @param desiredRole the desired role
     * @param employmentType the employment type
     * @param student the student
     * @param githubProfileUrl the github profile url
     * @return the validated existing candidate, or the newly persisted candidate
     */
    private User findOrCreateCandidate(
            String firstName,
            String lastName,
            String email,
            String currentTitle,
            String desiredRole,
            EmploymentType employmentType,
            boolean student,
            String githubProfileUrl) {
        String legacyGithubProfileUrl = "https://github.com/shigoto-demo-" + firstName.toLowerCase();
        return userRepository.findByEmail(email)
                .map(existing -> {
                    User candidate = validateDemoUser(existing, firstName, lastName, Role.CANDIDATE, null);
                    boolean legacySparseProfile = isMissing(candidate.getCurrentTitle())
                            && isMissing(candidate.getDesiredRole())
                            && candidate.getEmploymentType() == null
                            && legacyGithubProfileUrl.equals(candidate.getGithubProfileUrl());
                    boolean changed = false;
                    if (isMissing(candidate.getGithubProfileUrl())
                            || legacyGithubProfileUrl.equals(candidate.getGithubProfileUrl())) {
                        candidate.setGithubProfileUrl(githubProfileUrl);
                        changed = true;
                    }
                    if (isMissing(candidate.getCurrentTitle())) {
                        candidate.setCurrentTitle(currentTitle);
                        changed = true;
                    }
                    if (isMissing(candidate.getDesiredRole())) {
                        candidate.setDesiredRole(desiredRole);
                        changed = true;
                    }
                    if (candidate.getEmploymentType() == null) {
                        candidate.setEmploymentType(employmentType);
                        changed = true;
                    }
                    if (legacySparseProfile && student && !candidate.isStudent()) {
                        candidate.setStudent(true);
                        changed = true;
                    }
                    return changed ? userRepository.save(candidate) : candidate;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .password(passwordEncoder.encode(DEMO_PASSWORD))
                        .role(Role.CANDIDATE)
                        .githubProfileUrl(githubProfileUrl)
                        .currentTitle(currentTitle)
                        .desiredRole(desiredRole)
                        .employmentType(employmentType)
                        .student(student)
                        .build()));
    }

    /**
     * Checks whether a demo text field has no usable value.
     * @param value the value to validate or normalize
     * @return {@code true} when the value is {@code null} or blank; otherwise {@code false}
     */
    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Reuses and validates a user identified by email, or persists the configured demo user.
     * @param firstName the first name
     * @param lastName the last name
     * @param email the email address
     * @param role the required user role
     * @param company the company that scopes the operation
     * @return the validated existing user, or the newly persisted user
     */
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

    /**
     * Reuses and validates the company HR account, or persists it when absent.
     * @param firstName the first name
     * @param lastName the last name
     * @param email the email address
     * @param company the company that scopes the operation
     * @return the existing or newly persisted HR user for the company
     */
    private User findOrCreateCompanyHr(
            String firstName, String lastName, String email, Company company) {
        var existingHrs = userRepository.findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(Role.HR, company);
        if (existingHrs.size() > 1) {
            throw new IllegalStateException("Company " + company.getName()
                    + " already has multiple HR users; manual cleanup is required");
        }
        if (existingHrs.size() == 1) {
            return existingHrs.getFirst();
        }
        return findOrCreateUser(firstName, lastName, email, Role.HR, company);
    }

    /**
     * Validates demo user against the applicable domain rules.
     * @param user the user to validate
     * @param firstName the first name
     * @param lastName the last name
     * @param role the required user role
     * @param expectedCompany the expected company
     * @return the existing user after its expected demo identity and company are verified
     */
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

    /**
     * Reuses a matching company job or persists the configured demo job.
     * @param company the company that scopes the operation
     * @param title the title
     * @param description the description
     * @param location the location
     * @return the matching existing job, or the newly persisted demo job
     */
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

    /**
     * Reuses a candidate-job application and repairs its demo fields, or creates it when absent.
     * @param candidate the candidate being processed
     * @param job the job
     * @param status the requested domain status
     * @param appliedAt the applied at
     * @param statusChangedAt the status changed at
     * @param demoCvStorageKey the demo cv storage key
     * @param demoCoverLetter the demo cover letter
     * @param customize the customize
     * @return the matching existing application after backfilling demo fields, or a newly persisted application
     */
    private Application findOrCreateApplication(
            User candidate,
            Job job,
            ApplicationStatus status,
            LocalDateTime appliedAt,
            LocalDateTime statusChangedAt,
            String demoCvStorageKey,
            String demoCoverLetter,
            Consumer<Application.ApplicationBuilder> customize) {
        return applicationRepository.findByCandidateIdAndJobId(candidate.getId(), job.getId())
                .map(existing -> populateMissingDemoApplicationFields(
                        existing, demoCvStorageKey, demoCoverLetter))
                .orElseGet(() -> {
                    Application.ApplicationBuilder builder = Application.builder()
                            .candidate(candidate)
                            .job(job)
                            .status(status)
                            .cvUrl(demoCvStorageKey)
                            .coverLetter(demoCoverLetter)
                            .appliedAt(appliedAt)
                            .statusChangedAt(statusChangedAt);
                    customize.accept(builder);
                    return applicationRepository.save(builder.build());
                });
    }

    /**
     * Backfills missing CV metadata and cover-letter content on an existing demo application.
     * @param application the application being processed
     * @param demoCvStorageKey the demo cv storage key
     * @param demoCoverLetter the demo cover letter
     * @return the application with any missing demo CV metadata and cover letter populated
     */
    private Application populateMissingDemoApplicationFields(
            Application application, String demoCvStorageKey, String demoCoverLetter) {
        boolean changed = false;
        if (demoCvStorageKey != null && application.getCvUrl() == null) {
            application.setCvUrl(demoCvStorageKey);
            changed = true;
        }
        if (demoCoverLetter != null && isMissing(application.getCoverLetter())) {
            application.setCoverLetter(demoCoverLetter);
            changed = true;
        }
        return changed ? applicationRepository.save(application) : application;
    }

    /**
     * Replaces a missing or legacy demo task repository URL with the intended value.
     * @param application the application being processed
     * @param legacyRepositoryUrl the legacy repository url
     * @param intendedRepositoryUrl the intended repository url
     * @return the application with its intended demo task repository URL
     */
    private Application populateDemoTaskRepositoryIfMissingOrLegacy(
            Application application, String legacyRepositoryUrl, String intendedRepositoryUrl) {
        if (isMissing(application.getTaskRepoUrl())
                || legacyRepositoryUrl.equals(application.getTaskRepoUrl())) {
            application.setTaskRepoUrl(intendedRepositoryUrl);
            return applicationRepository.save(application);
        }
        return application;
    }

    /**
     * Assigns the configured reviewer when the demo application does not already reference one.
     * @param application the application being processed
     * @param reviewer the reviewer
     * @return the application with the reviewer assignment applied when needed
     */
    private Application assignDemoTaskReviewer(Application application, User reviewer) {
        if (application.getTaskReviewer() == null) {
            application.setTaskReviewer(reviewer);
        }
        return application;
    }

    /**
     * Reuses an application interview of the requested type, or persists the configured demo interview.
     * @param application the application being processed
     * @param interviewer the authenticated interviewer
     * @param type the requested domain type
     * @param status the requested domain status
     * @param scheduledAt the scheduled at
     * @param meetingLink the meeting link
     * @param feedback the feedback text
     * @return the matching existing interview, or the newly persisted demo interview
     */
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
