package com.careerai.builder.config;

import com.careerai.builder.domain.entity.Company;
import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.repository.CompanyRepository;
import com.careerai.builder.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * Seeds the database with realistic Job Descriptions for RAG pipeline testing.
 * Runs after MockJobSeeder (which seeds legacy skill-based jobs).
 * Only seeds if no RAG-ready jobs exist (jobs with role + level + rawDescription).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
@Order(2)  // Run after MockJobSeeder
public class RagJobSeeder implements CommandLineRunner {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    @Override
    public void run(String... args) {
        // Check if RAG jobs already exist
        boolean hasRagJobs = jobRepository.findAll().stream()
                .anyMatch(j -> j.getRawDescription() != null && !j.getRawDescription().isBlank()
                        && j.getRole() != null && j.getLevel() != null);

        if (hasRagJobs) {
            log.info("RAG-ready jobs already exist, skipping seed.");
            return;
        }

        log.info("🌱 Seeding RAG Job Data (20+ diverse JDs)...");

        // Companies
        Company techglobal = upsertCompany("TechGlobal", "Enterprise Software", "enterprise");
        Company finsmart = upsertCompany("FinSmart", "Fintech", "mid");
        Company creativeui = upsertCompany("CreativeUI", "Design Agency", "startup");
        Company cloudnine = upsertCompany("CloudNine", "Cloud Infrastructure", "enterprise");
        Company datavault = upsertCompany("DataVault", "Data Analytics", "mid");
        Company shopflow = upsertCompany("ShopFlow", "E-commerce", "mid");
        Company edutopia = upsertCompany("Edutopia", "EdTech", "startup");
        Company healthbridge = upsertCompany("HealthBridge", "HealthTech", "mid");

        // ─── BACKEND JOBS ───────────────────────────────────────────────
        seedJob(techglobal, "Java Backend Developer", "backend", "junior", "Ho Chi Minh City", "15-22M",
                """
                We are looking for a Junior Java Backend Developer to join our core platform team.
                
                Responsibilities:
                - Develop and maintain RESTful APIs using Spring Boot
                - Write clean, testable code with proper unit and integration tests
                - Collaborate with frontend teams on API contract design
                - Participate in code reviews and agile ceremonies
                
                Required Skills:
                - Java 17+ with solid understanding of OOP principles
                - Spring Boot, Spring Data JPA, Spring Security basics
                - Relational databases (PostgreSQL or MySQL)
                - Git version control
                - Basic understanding of REST API design
                
                Nice to have:
                - Docker and containerization
                - CI/CD pipeline experience
                - Message queues (RabbitMQ, Kafka)
                """);

        seedJob(finsmart, "Senior Backend Engineer", "backend", "senior", "Ho Chi Minh City", "40-60M",
                """
                FinSmart is building the next generation of financial infrastructure for Southeast Asia.
                We need a Senior Backend Engineer to lead the development of our payment processing platform.
                
                Responsibilities:
                - Design and implement high-throughput, low-latency payment APIs
                - Architect microservices with event-driven patterns
                - Mentor junior engineers and drive technical standards
                - Own the reliability and scalability of critical financial systems
                
                Required Skills:
                - 5+ years of backend development experience
                - Expert in Java or Kotlin with Spring Boot ecosystem
                - Deep experience with PostgreSQL, Redis, and message brokers (Kafka)
                - Strong understanding of distributed systems and event sourcing
                - Docker, Kubernetes, and cloud platforms (AWS/GCP)
                - Experience with payment gateways and financial compliance
                
                Nice to have:
                - Experience with gRPC and Protocol Buffers
                - Knowledge of PCI-DSS compliance
                - Contributions to open-source projects
                """);

        seedJob(cloudnine, "Backend Developer (Node.js)", "backend", "mid", "Remote", "25-40M",
                """
                CloudNine is a fully remote company building developer tools for cloud infrastructure.
                
                Responsibilities:
                - Build and maintain Node.js microservices for our CLI and dashboard
                - Design database schemas and optimize query performance
                - Implement authentication and authorization flows
                - Write comprehensive tests and documentation
                
                Required Skills:
                - 3+ years with Node.js and TypeScript
                - Express.js or Fastify framework experience
                - PostgreSQL and MongoDB
                - REST API and GraphQL design
                - AWS services (Lambda, SQS, DynamoDB)
                - Git, Docker, GitHub Actions
                
                Nice to have:
                - Terraform or Pulumi for IaC
                - Monitoring with Datadog or New Relic
                """);

        seedJob(healthbridge, "Python Backend Developer", "backend", "mid", "Ha Noi", "25-35M",
                """
                HealthBridge is digitizing healthcare workflows across Vietnam.
                
                Responsibilities:
                - Develop backend services for patient management systems
                - Build RESTful APIs using FastAPI or Django
                - Integrate with third-party health information systems (HL7, FHIR)
                - Ensure data security and HIPAA-like compliance
                
                Required Skills:
                - 2+ years Python backend development
                - FastAPI or Django REST Framework
                - PostgreSQL, SQLAlchemy
                - Basic DevOps (Docker, Linux)
                - Understanding of healthcare data standards is a plus
                
                Nice to have:
                - Experience with machine learning pipelines
                - Celery for background task processing
                """);

        // ─── FRONTEND JOBS ──────────────────────────────────────────────
        seedJob(creativeui, "Senior Frontend Developer (React)", "frontend", "senior", "Remote", "35-50M",
                """
                CreativeUI builds beautiful, performant web experiences for premium brands.
                
                Responsibilities:
                - Lead frontend architecture decisions for high-traffic web applications
                - Build reusable component libraries with React and TypeScript
                - Implement responsive designs with modern CSS (Tailwind, CSS Modules)
                - Optimize Core Web Vitals and application performance
                - Mentor junior developers on best practices
                
                Required Skills:
                - 4+ years of React development with TypeScript
                - Next.js (App Router, SSR, ISR)
                - State management (Zustand, Redux Toolkit, or React Query)
                - CSS-in-JS or Tailwind CSS
                - Testing with Jest, React Testing Library, Playwright
                - Git, CI/CD, Vercel or similar platforms
                
                Nice to have:
                - Design system experience (Storybook)
                - Animation libraries (Framer Motion, GSAP)
                - Accessibility (WCAG 2.1)
                """);

        seedJob(shopflow, "Junior Frontend Developer", "frontend", "junior", "Ho Chi Minh City", "12-18M",
                """
                ShopFlow is an e-commerce platform serving 10,000+ merchants in Vietnam.
                
                Responsibilities:
                - Build and maintain merchant dashboard features using React
                - Implement pixel-perfect UI from Figma designs
                - Write clean, maintainable component code
                - Fix bugs and improve existing features
                
                Required Skills:
                - Solid understanding of HTML, CSS, JavaScript
                - React basics (hooks, context, component lifecycle)
                - Basic TypeScript
                - Responsive design principles
                - Git version control
                
                Nice to have:
                - Next.js experience
                - REST API integration with Axios or Fetch
                - Tailwind CSS
                """);

        seedJob(edutopia, "Frontend Engineer (Vue.js)", "frontend", "mid", "Da Nang", "20-30M",
                """
                Edutopia is building an interactive learning platform for K-12 students.
                
                Responsibilities:
                - Develop interactive learning modules with Vue.js 3
                - Build rich text editors and drag-and-drop interfaces
                - Integrate with backend APIs for content management
                - Ensure cross-browser compatibility and mobile responsiveness
                
                Required Skills:
                - 2+ years Vue.js development (Composition API)
                - Vuex or Pinia for state management
                - TypeScript
                - HTML5 Canvas or SVG for interactive visuals
                - REST API integration
                
                Nice to have:
                - Nuxt.js for SSR
                - WebSocket experience for real-time features
                - Experience with e-learning platforms
                """);

        // ─── FULLSTACK JOBS ─────────────────────────────────────────────
        seedJob(techglobal, "Fullstack Developer", "fullstack", "mid", "Ho Chi Minh City", "25-35M",
                """
                Join our product team to build end-to-end features for our enterprise CRM platform.
                
                Responsibilities:
                - Develop full features from database to UI
                - Build APIs with Spring Boot and consume them in React
                - Write database migrations and optimize queries
                - Participate in product design discussions
                
                Required Skills:
                - Java with Spring Boot for backend
                - React with TypeScript for frontend
                - PostgreSQL
                - REST API design
                - Git, Docker basics
                
                Nice to have:
                - Redis caching
                - Message queues
                - CI/CD experience
                """);

        seedJob(shopflow, "Fullstack Engineer (MERN)", "fullstack", "mid", "Remote", "28-40M",
                """
                Build the next generation of our e-commerce platform with the MERN stack.
                
                Responsibilities:
                - Develop features across the entire stack (MongoDB, Express, React, Node.js)
                - Design and implement RESTful APIs
                - Build responsive, accessible frontend components
                - Deploy and monitor applications on AWS
                
                Required Skills:
                - 3+ years fullstack development
                - Node.js with Express or NestJS
                - React with hooks and modern patterns
                - MongoDB and Redis
                - TypeScript
                - AWS (EC2, S3, CloudFront)
                
                Nice to have:
                - GraphQL
                - Docker and Kubernetes
                - Performance optimization
                """);

        // ─── DEVOPS JOBS ────────────────────────────────────────────────
        seedJob(cloudnine, "DevOps Engineer", "devops", "mid", "Remote", "30-45M",
                """
                CloudNine needs a DevOps engineer to build and maintain our cloud infrastructure.
                
                Responsibilities:
                - Design and maintain CI/CD pipelines (GitHub Actions, Jenkins)
                - Manage Kubernetes clusters on AWS EKS
                - Implement infrastructure as code with Terraform
                - Set up monitoring, alerting, and logging (Prometheus, Grafana, ELK)
                - Ensure security best practices and compliance
                
                Required Skills:
                - 3+ years DevOps/SRE experience
                - AWS services (EKS, ECR, S3, RDS, CloudWatch)
                - Docker and Kubernetes
                - Terraform or CloudFormation
                - Linux administration
                - Scripting (Bash, Python)
                - CI/CD pipeline design
                
                Nice to have:
                - Service mesh (Istio)
                - GitOps with ArgoCD
                - Cost optimization strategies
                """);

        seedJob(finsmart, "Junior DevOps Engineer", "devops", "junior", "Ho Chi Minh City", "15-22M",
                """
                Join our platform team to learn and grow as a DevOps engineer.
                
                Responsibilities:
                - Maintain existing CI/CD pipelines
                - Help manage Docker containers and deployments
                - Monitor system health and respond to alerts
                - Write automation scripts
                
                Required Skills:
                - Basic Linux administration
                - Docker fundamentals
                - Git version control
                - Basic scripting (Bash or Python)
                - Understanding of networking basics
                
                Nice to have:
                - AWS or GCP basics
                - Kubernetes basics
                - Ansible or Terraform
                """);

        // ─── DATA/AI JOBS ───────────────────────────────────────────────
        seedJob(datavault, "Data Engineer", "data", "mid", "Ha Noi", "30-45M",
                """
                DataVault is building a real-time analytics platform for retail businesses.
                
                Responsibilities:
                - Design and build ETL/ELT data pipelines
                - Manage data warehouses (BigQuery, Redshift, or Snowflake)
                - Build real-time streaming pipelines with Apache Kafka
                - Ensure data quality and implement data governance practices
                
                Required Skills:
                - 3+ years data engineering experience
                - Python and SQL
                - Apache Spark or Flink
                - Kafka or Pulsar for streaming
                - Data warehousing (BigQuery, Snowflake)
                - Airflow or Dagster for orchestration
                - Docker, Git
                
                Nice to have:
                - dbt for data transformation
                - Experience with data mesh architecture
                - Machine learning pipelines (MLflow)
                """);

        seedJob(datavault, "Junior Data Analyst", "data", "junior", "Ho Chi Minh City", "12-18M",
                """
                Help our clients understand their data and make better business decisions.
                
                Responsibilities:
                - Create dashboards and reports using Metabase or Looker
                - Write SQL queries to extract business insights
                - Clean and transform data for analysis
                - Present findings to stakeholders
                
                Required Skills:
                - Strong SQL skills
                - Excel/Google Sheets proficiency
                - Basic Python or R for data analysis
                - Data visualization tools (Metabase, Tableau, Power BI)
                - Analytical thinking and communication skills
                
                Nice to have:
                - Statistics fundamentals
                - A/B testing experience
                - Basic machine learning concepts
                """);

        seedJob(healthbridge, "AI/ML Engineer", "data", "senior", "Remote", "50-80M",
                """
                HealthBridge is applying AI to revolutionize medical diagnostics.
                
                Responsibilities:
                - Develop and deploy machine learning models for medical image analysis
                - Build MLOps pipelines for model training, evaluation, and deployment
                - Research and implement state-of-the-art deep learning architectures
                - Collaborate with medical professionals to validate model outputs
                
                Required Skills:
                - 4+ years ML/DL experience
                - Python, PyTorch or TensorFlow
                - Computer vision (CNNs, transformers)
                - MLOps tools (MLflow, Kubeflow, W&B)
                - Docker, Kubernetes
                - Strong mathematical foundation (linear algebra, probability)
                
                Nice to have:
                - Medical imaging experience (DICOM)
                - NLP experience
                - Published research papers
                """);

        // ─── MOBILE JOBS ────────────────────────────────────────────────
        seedJob(shopflow, "Mobile Developer (React Native)", "mobile", "mid", "Ho Chi Minh City", "25-38M",
                """
                Build our merchant mobile app used by 10,000+ shop owners daily.
                
                Responsibilities:
                - Develop cross-platform mobile features with React Native
                - Integrate with REST APIs and push notification services
                - Optimize app performance and reduce crash rates
                - Publish and manage app store releases
                
                Required Skills:
                - 2+ years React Native development
                - TypeScript
                - React Navigation
                - REST API integration
                - State management (Redux or Zustand)
                - iOS and Android platform knowledge
                
                Nice to have:
                - Native module development (Swift/Kotlin)
                - Firebase (Analytics, Crashlytics)
                - App Store optimization
                """);

        seedJob(edutopia, "iOS Developer (Swift)", "mobile", "junior", "Ha Noi", "15-25M",
                """
                Build engaging learning experiences on iOS for millions of students.
                
                Responsibilities:
                - Develop iOS app features using Swift and SwiftUI
                - Implement smooth animations and transitions
                - Integrate with backend APIs
                - Write unit and UI tests
                
                Required Skills:
                - Swift programming language
                - SwiftUI or UIKit
                - Understanding of iOS app lifecycle
                - REST API integration
                - Git version control
                
                Nice to have:
                - Core Data or Realm
                - Combine framework
                - App Store submission experience
                """);

        // ─── EXTRA VARIETY ──────────────────────────────────────────────
        seedJob(techglobal, "QA Automation Engineer", "backend", "mid", "Ho Chi Minh City", "22-32M",
                """
                Ensure the quality and reliability of our enterprise platform through test automation.
                
                Responsibilities:
                - Design and implement automated test frameworks
                - Write API tests, integration tests, and E2E tests
                - Set up CI/CD test pipelines
                - Collaborate with developers to improve code quality
                
                Required Skills:
                - 2+ years QA automation experience
                - Java or Python for test scripting
                - Selenium, Playwright, or Cypress
                - REST API testing (Postman, RestAssured)
                - SQL for test data management
                - Git, Jenkins or GitHub Actions
                
                Nice to have:
                - Performance testing (JMeter, k6)
                - Contract testing (Pact)
                - Docker for test environments
                """);

        seedJob(finsmart, "Tech Lead — Backend", "backend", "lead", "Ho Chi Minh City", "60-90M",
                """
                Lead the backend engineering team at FinSmart as we scale to serve millions of transactions.
                
                Responsibilities:
                - Set technical direction and architecture for the backend platform
                - Lead a team of 5-8 backend engineers
                - Drive code quality through reviews, standards, and mentorship
                - Collaborate with Product and Design on feature planning
                - Own system reliability, performance, and scalability
                
                Required Skills:
                - 7+ years backend development, 2+ years in a lead role
                - Expert in Java/Kotlin with Spring Boot ecosystem
                - Deep understanding of microservices architecture
                - PostgreSQL, Redis, Kafka at scale
                - AWS/GCP cloud architecture
                - Strong communication and leadership skills
                
                Nice to have:
                - FinTech domain experience
                - System design interview experience
                - Open-source contributions
                """);

        seedJob(cloudnine, "Site Reliability Engineer", "devops", "senior", "Remote", "45-70M",
                """
                Keep CloudNine's infrastructure running at 99.99% uptime.
                
                Responsibilities:
                - Design and implement high-availability architectures
                - Build observability stack (metrics, logs, traces)
                - Automate incident response and runbooks
                - Capacity planning and cost optimization
                - On-call rotation and incident management
                
                Required Skills:
                - 5+ years SRE/DevOps experience
                - AWS or GCP at enterprise scale
                - Kubernetes, Helm, and service mesh
                - Prometheus, Grafana, OpenTelemetry
                - Terraform and GitOps (ArgoCD)
                - Programming (Go, Python, or Java)
                - Incident management and postmortem culture
                
                Nice to have:
                - Chaos engineering experience
                - eBPF for observability
                - Multi-cloud strategies
                """);

        log.info("✅ Seeded {} RAG-ready jobs with diverse roles and levels", 20);
    }

    private Company upsertCompany(String name, String industry, String size) {
        return companyRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .name(name)
                        .industry(industry)
                        .size(size)
                        .build()));
    }

    private void seedJob(Company company, String title, String role, String level,
                         String location, String salary, String rawDescription) {
        jobRepository.save(Job.builder()
                .title(title)
                .company(company.getName())
                .companyEntity(company)
                .location(location)
                .role(role)
                .level(level)
                .salaryRange(salary)
                .rawDescription(rawDescription.trim())
                .descriptionHtml(rawDescription.trim())
                .sourceUrl("https://careers." + company.getName().toLowerCase().replaceAll("\\s+", "") + ".com/jobs")
                .ingestionStatus(Job.IngestionStatus.PENDING)
                .build());
    }
}
