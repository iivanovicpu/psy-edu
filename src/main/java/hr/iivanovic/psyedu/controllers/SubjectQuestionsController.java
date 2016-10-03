package hr.iivanovic.psyedu.controllers;

import static hr.iivanovic.psyedu.util.JsonUtil.dataToJson;
import static hr.iivanovic.psyedu.util.RequestUtil.clientAcceptsHtml;
import static hr.iivanovic.psyedu.util.RequestUtil.clientAcceptsJson;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hr.iivanovic.psyedu.AppConfiguration;
import hr.iivanovic.psyedu.db.Question;
import hr.iivanovic.psyedu.db.Subject;
import hr.iivanovic.psyedu.html.HtmlParser;
import hr.iivanovic.psyedu.html.TitleLink;
import hr.iivanovic.psyedu.util.Path;
import hr.iivanovic.psyedu.util.ViewUtil;
import lombok.Data;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * @author iivanovic
 * @date 26.09.16.
 */
public class SubjectQuestionsController extends AbstractController {
    private static HtmlParser htmlParser = HtmlParser.getInstance();

    public static Route fetchtitlesForAddQuestions = (Request request, Response response) -> {
        LoginController.ensureUserIsLoggedIn(request, response);
        if (!LoginController.isEditAllowed(request)) {
            return ViewUtil.notAcceptable.handle(request, response);
        }
        long id = Long.parseLong(request.params("id"));
        if (clientAcceptsHtml(request)) {
            HashMap<String, Object> model = new HashMap<>();
            Subject subject = dbProvider.getSubject(id);
            model.put("subject", subject);
            model.put("successmsg", "");
            File file = new File(AppConfiguration.getInstance().getExternalLocation() + subject.getUrl());
            List<TitleLink> titleLinks = htmlParser.getAllSubjectsLinks(file, subject.getUrl(), subject.getId());
            model.put("titles", titleLinks);
            return ViewUtil.render(request, model, Path.Template.SUBJECTS_ONE_QUESTIONS);
        }
        if (clientAcceptsJson(request)) {
            return dataToJson(dbProvider.getSubject(id));
        }
        return ViewUtil.notAcceptable.handle(request, response);
    };

    public static Route fetchOneTitleForAddQuestions = (Request request, Response response) -> {
        LoginController.ensureUserIsLoggedIn(request, response);
        String id = request.params("id");
        int subjectId = Integer.parseInt(request.params("subjectid"));
        System.out.println(id + " " + subjectId);
        if (clientAcceptsHtml(request)) {
            HashMap<String, Object> model = new HashMap<>();
            model.put("validation",false);
            model.put("subjectId",subjectId);
            model.put("titleId",id);
            List<Question> questions = dbProvider.getAllQuestionsForSubjectAndTitle(subjectId, id);
            String htmlQuestions = renderQuestions(questions);

            model.put("questions", htmlQuestions);

            Subject subject = dbProvider.getSubject(subjectId);
            String content = htmlParser.getOneTitleContent(AppConfiguration.getInstance().getExternalLocation() + subject.getUrl().substring(1,subject.getUrl().length()), id);
            model.put("content",content);
            return ViewUtil.render(request, model, Path.Template.SUBJECT_ONE_TITLE_QUESTIONS);
        }
        return ViewUtil.notAcceptable.handle(request, response);
    };

    public static Route submitQuestion = (Request request, Response response) -> {
        LoginController.ensureUserIsLoggedIn(request, response);
        if (clientAcceptsHtml(request)) {
            int subjectId = Integer.parseInt(request.queryParams("subjectid"));
            String titleId = request.queryParams("titleid");
            HashMap<String, Object> model = new HashMap<>();
            ValidationResult validationResult = validatedQuestion(request);
            Question question = validationResult.getQuestion();
            Subject subject = dbProvider.getSubject(subjectId);
            String content = htmlParser.getOneTitleContent(AppConfiguration.getInstance().getExternalLocation() + subject.getUrl().substring(1,subject.getUrl().length()), titleId);
            model.put("content",content);
            model.put("validation",validationResult.getValidationMessage());
            model.put("subjectId", question.getSubjectId());
            model.put("titleId", titleId);
            if(validationResult.isValid()){
                model.put("validation", false);
                System.out.println(validationResult);
                dbProvider.createQuestion(validationResult.question);
            }
            List<Question> questions = dbProvider.getAllQuestionsForSubjectAndTitle(subjectId, titleId);
            String htmlQuestions = renderQuestions(questions);
            model.put("questions", htmlQuestions);
            model.put("validation", validationResult.getValidationMessage());
            System.out.println("posted: " + question.getTitleId() + " " + question.getSubjectId());
            return ViewUtil.render(request, model, Path.Template.SUBJECT_ONE_TITLE_QUESTIONS);
        }
        return ViewUtil.notAcceptable.handle(request, response);
    };

    private static String renderQuestions(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        for (Question question : questions) {
            sb.append("<hr>").append(question.getQuestion());
            QuestionType questionType = getQuestionType(question.getAnswers());
            String[] answers = question.getAnswers().split("\\|");
            for (int i = 0; i < answers.length; i++) {
                String answer = answers[i].replaceAll("\\*","");
                if(QuestionType.SELECT.equals(questionType)) {
                    sb.append("<br><input type=\"checkbox\" name=\"").append(question.getId()).append("_").append(i).append("\" value=\"").append(answer).append("\">").append(answer);
                }
                if(QuestionType.ENTER_ANSWER.equals(questionType)){
                    sb.append("<br><input type=\"text\" name=\"").append(question.getId()).append("_").append(i).append("\">");
                }
                if(QuestionType.ENTER_DESCRIPTIVE.equals(questionType)){
                    sb.append("<br><textarea name=\"").append(question.getId()).append("_").append(i).append("\" rows=\"10\" cols=\"30\"></textarea>");
                }
            }
        }
        return sb.toString();
    }

    private static QuestionType getQuestionType(String answers) {
        Pattern patternCorrect = Pattern.compile("\\*");
        Pattern patternSelections = Pattern.compile("\\|");
        Matcher matcher = patternCorrect.matcher(answers);
        int countCorrect = 0;
        while (matcher.find())
            countCorrect++;

        int countSelections = 0;
        matcher = patternSelections.matcher(answers);
        while (matcher.find())
            countSelections++;

        if (countCorrect >= 1 && countSelections >= 1)
            return QuestionType.SELECT;
        if (countCorrect == 1 && countSelections == 0)
            return QuestionType.ENTER_ANSWER;
        return QuestionType.ENTER_DESCRIPTIVE;
    }

    private static ValidationResult validatedQuestion(Request request) {
        ValidationResult result = new ValidationResult();
        StringBuilder sb = new StringBuilder();
        String titleId = request.queryParams("titleid");
        int subjectid;
        int points;
        if(null == titleId || titleId.isEmpty()){
            sb.append("\nheader data corrupt - titleId (0)");
            result.setValid(false);
        }
        try {
            subjectid = Integer.parseInt(request.queryParams("subjectid"));
            if(subjectid == 0){
                sb.append("\nheader data corrupt - SubjectId (0)");
                result.setValid(false);
            }
        } catch (NumberFormatException e){
            sb.append("\nheader data corrupt - subjectId +(" ).append(request.queryParams("subjectId")).append(")");
            result.setValid(false);
            throw e;
        }
        String question = request.queryParams("question");
        if(null == question || question.isEmpty()){
            sb.append("\nPitanje - obavezan unos");
            result.setValid(false);
        }
        String answers = request.queryParams("answers");
        if(null == answers || answers.isEmpty()){
            sb.append("\nOdgovori - obavezan podatak");
            result.setValid(false);
        }
        try {
            points = Integer.parseInt(request.queryParams("points"));
        } catch (NumberFormatException e){
            sb.append("\nBodovi - mora biti brojčana vrijednost");
            result.setValid(false);
            throw e;
        }
        if(result.isValid()){
            result.setQuestion(new Question(subjectid, titleId, question, answers, points));
            return result;
        } else {
            result.setQuestion(new Question(subjectid, titleId, question, answers, points));
            result.setValidationMessage(sb.toString());
            return result;
        }
    }

    @Data
    private static class ValidationResult {
        Question question;
        boolean valid = true;
        String validationMessage;
    }
}
