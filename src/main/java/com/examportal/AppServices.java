package com.examportal;

import com.examportal.service.*;

/** Simple static service locator used to pass services between views without a DI framework. */
public final class AppServices {

    private static AuthService      authService;
    private static ExamService      examService;
    private static EvaluatorService evaluatorService;
    private static ResultService    resultService;
    private static ProctorService   proctorService;

    private AppServices() {}

    public static void init(AuthService auth, ExamService exam,
                            EvaluatorService evaluator, ResultService result,
                            ProctorService proctor) {
        authService      = auth;
        examService      = exam;
        evaluatorService = evaluator;
        resultService    = result;
        proctorService   = proctor;
    }

    public static AuthService      auth()      { return authService; }
    public static ExamService      exam()      { return examService; }
    public static EvaluatorService evaluator() { return evaluatorService; }
    public static ResultService    result()    { return resultService; }
    public static ProctorService   proctor()   { return proctorService; }
}
