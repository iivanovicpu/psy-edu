package hr.iivanovic.psyedu.db;

import hr.iivanovic.psyedu.util.DbUtil;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Sql2oModel implements Model {

    private static Sql2oModel instance = null;

    private Sql2o sql2o;

    public static synchronized Sql2oModel getInstance() {
        if (null == instance) {
            instance = new Sql2oModel();
        }
        return instance;
    }

    private Sql2oModel() {
        this.sql2o = DbUtil.getPostgreSQLDataSource();
    }

    public void clearRecordsForReinit() {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("delete from subjects").executeUpdate();
            conn.createQuery("delete from users").executeUpdate();
            conn.createQuery("delete from questions").executeUpdate();
            conn.createQuery("delete from learning_log").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createSubject(String title, String keywords, String url, SubjectLevel subjectLevel, int subjectPositionId) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("insert into subjects(title, keywords, url, subject_level_id, content, additional_content, subject_position_id) VALUES ( :title, :keywords, :url, :levelid, '<html></html>','<html></html>', :subjectPositionId)")
                    .addParameter("title", title)
                    .addParameter("keywords", keywords)
                    .addParameter("url", url)
                    .addParameter("levelid", subjectLevel.getId())
                    .addParameter("subjectPositionId", subjectPositionId)
                    .executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createSubSubject(Subject subject) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("insert into subjects ( title, keywords, url, subject_id, parent_subject_id, subject_level_id, ordinal_number, content, additional_content, summary_goals, subject_position_id) " +
                    "                       VALUES (:title, :keywords, :url, :subjectId, :parentSubjectId, :subjectLevelId, :order, :content, :additionalContent, :summaryAndGoals, :subjectPositionId)")
                    .addParameter("title", subject.getTitle())
                    .addParameter("keywords", subject.getKeywords())
                    .addParameter("url", subject.getUrl())
                    .addParameter("subjectId", subject.getSubjectId())
                    .addParameter("parentSubjectId", subject.getParentSubjectId())
                    .addParameter("subjectLevelId", subject.getSubjectLevelId())
                    .addParameter("order", subject.getOrder())
                    .addParameter("content", subject.getContent())
                    .addParameter("additionalContent", subject.getAdditionalContent())
                    .addParameter("summaryAndGoals", subject.getSummaryAndGoals())
                    .addParameter("subjectPositionId", subject.getSubjectPosition().getId())
                    .executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void createUser(String username, String password, String firstName, String lastName, String email, String status) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("insert into users(username, password, firstName, lastNAme, email, status) VALUES (:username, :password, :firstname, :lastname, :email, :status)")
                    .addParameter("username", username)
                    .addParameter("password", password)
                    .addParameter("firstname", firstName)
                    .addParameter("lastname", lastName)
                    .addParameter("email", email)
                    .addParameter("status", status)
                    .executeUpdate();
        }
    }

    @Override
    public List<User> getAllUsers() {
        try (Connection conn = sql2o.open()) {
            List<User> users = conn.createQuery("select * from users")
                    .addColumnMapping("intelligence_verbal_points", "intelligencePointsVerbal")
                    .addColumnMapping("intelligence_not_verbal_points", "intelligencePointsNotVerbal")
                    .addColumnMapping("intelligence_math_logic_points", "intelligencePointsMathLogic")
                    .executeAndFetch(User.class);
            return users;
        }
    }

    @Override
    public String getRandomMotivationalMessage() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("SELECT message FROM motivational_messages OFFSET floor(random()*(select count(*) from motivational_messages)) LIMIT 1;")
                    .executeAndFetchFirst(String.class);
        }
    }

    @Override
    public User getUserByUsername(String username) {
        try (Connection conn = sql2o.open()) {
            User user = conn.createQuery("select * from users where username=:username")
                    .addParameter("username", username)
                    .addColumnMapping("intelligence_verbal_points", "intelligencePointsVerbal")
                    .addColumnMapping("intelligence_not_verbal_points", "intelligencePointsNotVerbal")
                    .addColumnMapping("intelligence_math_logic_points", "intelligencePointsMathLogic")
                    .executeAndFetchFirst(User.class);
            return user;
        }
    }

    @Override
    public List<Subject> getAllSubjects() {
        try (Connection conn = sql2o.open()) {
            List<Subject> subjects = conn.createQuery("select * from subjects")
                    .addColumnMapping("subject_id", "subjectId")
                    .addColumnMapping("parent_subject_id", "parentSubjectId")
                    .addColumnMapping("subject_level_id", "subjectLevelId")
                    .addColumnMapping("ordinal_number", "order")
                    .addColumnMapping("additional_content", "additionalContent")
                    .addColumnMapping("subject_position_id", "subjectPositionId")
                    .addColumnMapping("summary_goals", "summaryAndGoals")
                    .executeAndFetch(Subject.class);
            return subjects;
        }
    }

    @Override
    public Subject getSubject(long id) {
        try (Connection conn = sql2o.open()) {
            Subject subject = conn.createQuery("select * from subjects where id=:id")
                    .addParameter("id", id)
                    .addColumnMapping("subject_id", "subjectId")
                    .addColumnMapping("parent_subject_id", "parentSubjectId")
                    .addColumnMapping("subject_level_id", "subjectLevelId")
                    .addColumnMapping("ordinal_number", "order")
                    .addColumnMapping("additional_content", "additionalContent")
                    .addColumnMapping("subject_position_id", "subjectPositionId")
                    .addColumnMapping("summary_goals", "summaryAndGoals")
                    .executeAndFetchFirst(Subject.class);
            if (null == subject.getParentSubjectId())
                subject.setParentSubjectId(subject.getId());
            return subject;
        }
    }

    @Override
    public void createStudentScore(int subjectId, int studentId, double result, boolean success) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("INSERT INTO STUDENT_SCORE (subjectId, studentId, result, count, success) VALUES (:subjectId, :studentId, :result, :count, :success);")
                    .addParameter("subjectId", subjectId)
                    .addParameter("studentId", studentId)
                    .addParameter("result", result)
                    .addParameter("count", 1)
                    .addParameter("success", success)
                    .executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateStudentScore(int subjectId, int studentId, double result, boolean success) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("UPDATE STUDENT_SCORE SET result = :result, count = count + 1, success = :success WHERE subjectId = :subjectId AND studentId = :studentId;")
                    .addParameter("subjectId", subjectId)
                    .addParameter("studentId", studentId)
                    .addParameter("result", result)
                    .addParameter("success", success)
                    .executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean studentScoreExists(int subjectId, int studentId) {
        try (Connection conn = sql2o.open()) {
            return 0 < conn.createQuery("SELECT count(*) FROM STUDENT_SCORE WHERE subjectId = :subjectId and studentId = :studentId;")
                    .addParameter("subjectId", subjectId)
                    .addParameter("studentId", studentId)
                    .executeAndFetchFirst(Integer.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void createQuestion(Question question) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("INSERT INTO questions (subjectId, question, questionTypeId, possibleAnswers, correctAnswers, points) VALUES (:subjectId, :question, :questionTypeId, :possibleAnswers, :correctAnswers, :points);")
                    .addParameter("subjectId", question.getSubjectId())
                    .addParameter("question", question.getQuestion())
                    .addParameter("questionTypeId", question.getQuestionTypeId())
                    .addParameter("possibleAnswers", question.getPossibleAnswers())
                    .addParameter("correctAnswers", question.getCorrectAnswers())
                    .addParameter("points", question.getPoints())
                    .executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteQuestion(int questionId) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("DELETE FROM questions WHERE id=:questionId;")
                    .addParameter("questionId", questionId)
                    .executeUpdate();
        }
    }

    @Override
    public List<Question> getAllQuestionsForSubject(int subjectId, boolean grouped) {
        if (grouped) {
            List<Question> groupedQuestions = new LinkedList<>();
            List<Integer> subjectIds = getSubjectsByParentSubjectId(subjectId).stream().map(Subject::getId).collect(Collectors.toList());
            try (Connection conn = sql2o.open()) {
                for (Integer subId : subjectIds) {
                    groupedQuestions.addAll(conn.createQuery("select * from questions where subjectId=:subjectId")
                            .addParameter("subjectId", subId)
                            .executeAndFetch(Question.class));
                }
            }
            return groupedQuestions;
        } else {
            try (Connection conn = sql2o.open()) {
                return conn.createQuery("select * from questions where subjectId=:subjectId")
                        .addParameter("subjectId", subjectId)
                        .executeAndFetch(Question.class);
            }
        }
    }

    @Override
    public Question getQuestionById(int questionId) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("select * from questions where id=:questionId")
                    .addParameter("questionId", questionId)
                    .executeAndFetchFirst(Question.class);
        }
    }

    @Override
    public List<ExternalLink> getAllExternalLinksBySubjectId(int subjectId) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("select id, title, url, subject_id, linktypeid from external_links where subject_id=:subjectId")
                    .addParameter("subjectId", subjectId)
                    .addColumnMapping("subject_id", "subjectId")
                    .addColumnMapping("linktypeid", "linkTypeId")
                    .executeAndFetch(ExternalLink.class);
        }
    }

    @Override
    public void createExternalLink(ExternalLink externalLink) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("insert into external_links (title, url, subject_id, linktypeid ) values (:title, :url, :subjectId, :linkTypeId);")
                    .addParameter("title", externalLink.getTitle())
                    .addParameter("url", externalLink.getUrl())
                    .addParameter("subjectId", externalLink.getSubjectId())
                    .addParameter("linkTypeId", externalLink.getLinkTypeId())
                    .executeUpdate();
        }
    }

    @Override
    public void deleteExternalLink(int id) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("delete from external_links where id = :id;")
                    .addParameter("id", id)
                    .executeUpdate();
        }
    }

    @Override
    public List<AdaptiveRuleDb> getAllAdaptiveRules() {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("select * from adaptive_rules")
                    .executeAndFetch(AdaptiveRuleDb.class);
        }
    }

    @Override
    public int nextIdx(String tag) {
        try (Connection conn = sql2o.open()) {
            return conn.createQuery("select nextval('all_id_seq')")
                    .executeAndFetchFirst(Integer.class);
        }
    }

    @Override
    public void save(User user) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("update users set color = :color where id = :id")
                    .addParameter("color", user.getColor())
                    .addParameter("id", user.getId())
                    .executeUpdate();
        }
    }

    @Override
    public void logLearningStatus(int studentId, int subjectId, int statusId) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("insert into LEARNING_LOG (studentId, subjectId, date, statusId ) values (:studentId, :subjectId, :date, :statusId);")
                    .addParameter("studentId", studentId)
                    .addParameter("subjectId", subjectId)
                    .addParameter("date", new Date())
                    .addParameter("statusId", statusId)
                    .executeUpdate();
        }
    }

    @Override
    public LearningLog getLearningLogStatus(int studentId, int subjectId) {
        try (Connection conn = sql2o.open()) {
            LearningLog learning = conn.createQuery("select * from LEARNING_LOG where studentId = :studentId and subjectId = :subjectId order by statusId desc;")
                    .addParameter("studentId", studentId)
                    .addParameter("subjectId", subjectId)
                    .executeAndFetchFirst(LearningLog.class);
            return learning;
        }
    }

    @Override
    public List<User> getAllStudents() {
        return getAllUsers().stream().filter(user -> user.getStatus().equals("STUDENT")).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public void updateStudentIntelligenceTypePoints(int userId, int intelligencePointsVerbal, int intelligencePointsNotVerbal, int intelligencePointsMathLogic) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("update users set intelligence_verbal_points = :intelligencePointsVerbal, " +
                    "intelligence_not_verbal_points = :intelligencePointsNotVerbal, " +
                    "intelligence_math_logic_points = :intelligencePointsMathLogic, " +
                    "completedIntelligencePoll = TRUE where id = :id")
                    .addParameter("intelligencePointsVerbal", intelligencePointsVerbal)
                    .addParameter("intelligencePointsNotVerbal", intelligencePointsNotVerbal)
                    .addParameter("intelligencePointsMathLogic", intelligencePointsMathLogic)
                    .addParameter("id", userId)
                    .executeUpdate();
        }
    }

    @Override
    public void decreaseIntelligenceTypePoints(int userId, IntelligenceType intelligenceType) {
        String querySegment = null;
        if (IntelligenceType.ML.equals(intelligenceType)) {
            querySegment = "intelligence_math_logic_points = intelligence_math_logic_points -1";
        }
        if (IntelligenceType.V.equals(intelligenceType)) {
            querySegment = "intelligence_verbal_points = intelligence_verbal_points -1";
        }
        if (IntelligenceType.NV.equals(intelligenceType)) {
            querySegment = "intelligence_not_verbal_points = intelligence_not_verbal_points -1";
        }
        if (null != querySegment) {
            try (Connection conn = sql2o.open()) {
                conn.createQuery("update users set " + querySegment + " where id = :id")
                        .addParameter("id", userId)
                        .executeUpdate();
            }
        }
    }

    @Override
    public User getUserById(int studentId) {
        return getAllUsers().stream().filter(user -> user.getId() == studentId).findFirst().orElse(null);
    }

    @Override
    public void updateStudentLearningStylePollResult(int id, int aktivni, int reflektivni, int opazajni, int intuitivni, int vizualni, int verbalni, int sekvencijalni, int globalni) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("update users set lsPointsActive = :lsPointsActive, lsPointsReflective = :lsPointsReflective, " +
                    "lsPointsVisual = :lsPointsVisual, lsPointsVerbal = :lsPointsVerbal, " +
                    "lsPointsSequential = :lsPointsSequential, lsPointsGlobal = :lsPointsGlobal, " +
                    "lsPointsIntuitive = :lsPointsIntuitive, lsPointsSensor = :lsPointsSensor, " +
                    "completedLearningStylePoll = TRUE where id = :id")
                    .addParameter("lsPointsActive", aktivni)
                    .addParameter("lsPointsReflective", reflektivni)
                    .addParameter("lsPointsVisual", vizualni)
                    .addParameter("lsPointsVerbal", verbalni)
                    .addParameter("lsPointsSequential", sekvencijalni)
                    .addParameter("lsPointsGlobal", globalni)
                    .addParameter("lsPointsIntuitive", intuitivni)
                    .addParameter("lsPointsSensor", opazajni)
                    .addParameter("id", id)
                    .executeUpdate();
        }
    }

    @Override
    public List<IntelligenceTypeDb> getAllIntelligenceTypes() {
        try (Connection conn = sql2o.open()) {
            List<IntelligenceTypeDb> intelligenceTypeDbs = conn.createQuery("select * from intelligence_types")
                    .executeAndFetch(IntelligenceTypeDb.class);
            for (IntelligenceTypeDb intelligenceTypeDb : intelligenceTypeDbs) {
                intelligenceTypeDb.setAdaptiveRules(getIntelligenceTypeRules(intelligenceTypeDb.getId()));
            }
            return intelligenceTypeDbs;
        }
    }

    @Override
    public List<AdaptiveRule> getIntelligenceTypeRules(int intelligenceTypeId) {
        List<AdaptiveRule> rules = new LinkedList<>();
        String sql = "SELECT adaptive_rule_id FROM intelligence_adaptive_rules where intelligence_type_id = :intelligenceTypeId";
        try (Connection con = sql2o.open()) {
            List<Integer> ruleIds = con.createQuery(sql)
                    .addParameter("intelligenceTypeId", intelligenceTypeId)
                    .executeScalarList(Integer.class);
            for (Integer ruleId : ruleIds) {
                rules.add(AdaptiveRule.getById(ruleId));
            }
            return rules;
        }
    }

    @Override
    public List<Subject> getSubjectsByParentSubjectId(int parentSubjectId) {
        try (Connection conn = sql2o.open()) {
            List<Subject> subjects = conn.createQuery("select * from subjects where id=:id or parent_subject_id=:id")
                    .addParameter("id", parentSubjectId)
                    .addColumnMapping("subject_id", "subjectId")
                    .addColumnMapping("parent_subject_id", "parentSubjectId")
                    .addColumnMapping("subject_level_id", "subjectLevelId")
                    .addColumnMapping("ordinal_number", "order")
                    .addColumnMapping("additional_content", "additionalContent")
                    .addColumnMapping("subject_position_id", "subjectPositionId")
                    .addColumnMapping("summary_goals", "summaryAndGoals")
                    .executeAndFetch(Subject.class);
            for (Subject subject : subjects) {
                if (null == subject.getParentSubjectId())
                    subject.setParentSubjectId(parentSubjectId);
            }
            return subjects.stream().sorted(Comparator.comparing(Subject::getId)).collect(Collectors.toList());
        }
    }

    @Override
    public List<Subject> getAllParentSubjects() {
        try (Connection conn = sql2o.open()) {
            List<Subject> subjects = conn.createQuery("select * from subjects where subject_id is null")
                    .addColumnMapping("subject_id", "subjectId")
                    .addColumnMapping("parent_subject_id", "parentSubjectId")
                    .addColumnMapping("subject_level_id", "subjectLevelId")
                    .addColumnMapping("ordinal_number", "order")
                    .addColumnMapping("additional_content", "additionalContent")
                    .addColumnMapping("subject_position_id", "subjectPositionId")
                    .addColumnMapping("summary_goals", "summaryAndGoals")
                    .executeAndFetch(Subject.class);
            return subjects;
        }

    }

    @Override
    public void updateSubject(Subject subject) {
        try (Connection conn = sql2o.open()) {
            conn.createQuery("update subjects set " +
                    "   title = :title, " +
                    "   keywords = :keywords, " +
                    "   url = :url, " +
                    "   subject_id = :subjectId, " +
                    "   parent_subject_id = :parentSubjectId, " +
                    "   subject_level_id = :subjectLevelId, " +
                    "   ordinal_number = :orderNumber, " +
                    "   content = :content, " +
                    "   additional_content = :additionalContent, " +
                    "   summary_goals = :summaryAndGoals " +
                    " where id = :id")
                    .addParameter("id", subject.getId())
                    .addParameter("subjectId", subject.getSubjectId())
                    .addParameter("parentSubjectId", subject.getParentSubjectId())
                    .addParameter("title", subject.getTitle())
                    .addParameter("keywords", subject.getKeywords())
                    .addParameter("content", subject.getContent())
                    .addParameter("additionalContent", subject.getAdditionalContent())
                    .addParameter("summaryAndGoals", subject.getSummaryAndGoals())
                    .addParameter("orderNumber", subject.getOrder())
                    .addParameter("subjectLevelId", subject.getSubjectLevelId())
                    .addParameter("url", subject.getUrl())
                    .executeUpdate();
        }

    }

    @Override
    public List<LearningStyleDb> getAllLearningStyles() {
        try (Connection conn = sql2o.open()) {
            List<LearningStyleDb> learningStyleDbs = conn.createQuery("select * from learning_styles")
                    .executeAndFetch(LearningStyleDb.class);
            for (LearningStyleDb learningStyleDb : learningStyleDbs) {
                learningStyleDb.setAdaptiveRules(getLearningStyleRules(learningStyleDb.getId()));
            }
            return learningStyleDbs;
        }
    }

    @Override
    public List<AdaptiveRule> getLearningStyleRules(int learningStyleId) {
        List<AdaptiveRule> rules = new LinkedList<>();
        String sql = "SELECT adaptive_rule_id FROM learning_styles_adaptive_rules where learning_style_id = :learningStyleId";
        try (Connection con = sql2o.open()) {
            List<Integer> ruleIds = con.createQuery(sql)
                    .addParameter("learningStyleId", learningStyleId)
                    .executeScalarList(Integer.class);
            for (Integer ruleId : ruleIds) {
                rules.add(AdaptiveRule.getById(ruleId));
            }
            return rules;
        }
    }

}