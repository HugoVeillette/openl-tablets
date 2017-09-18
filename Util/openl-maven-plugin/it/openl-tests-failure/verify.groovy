try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // Check that all tests are run
    assert lines.any { it.contains('Running GreetingTestTestAll from the module Simple Rules') }
    assert lines.any { it.contains('Running GreetingSuccessful1TestAll from the module Simple Rules') }
    assert lines.any { it.contains('Running GreetingSuccessful2TestAll from the module Simple Rules') }
    assert lines.any { it.contains('Running TryGreetingTestTestAll from the module Rules With Error') }
    assert lines.any { it.contains('Running TryFailedGreetingTestTestAll from the module Rules With Error') }

    // Check summary for failed test s
    assert lines.any { it.contains('Rules With Error.TryGreetingTestTestAll#4 expected: <Good Evening, World!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTestTestAll#1 expected: <Good Morning!> but was <Good Morning, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTestTestAll#2 expected: <Good Afternoon!> but was <Good Afternoon, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTestTestAll#3 expected: <Good Evening!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Rules With Error.TryFailedGreetingTestTestAll#4 expected: <Good Night!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Simple Rules.GreetingTestTestAll#3 expected: <Good Evening, World!> but was <Good Night, World!>') }
    assert lines.any { it.contains('Simple Rules.GreetingTestTestAll#5 expected: <Good Night, World!> but was <null>') }

    // Check summary for tests in error
    assert lines.any { it.contains('Rules With Error.TryGreetingTestTestAll#2 java.lang.ArithmeticException') }

    // Check total tests statistics
    assert lines.any { it.contains('Total tests run: 20, Failures: 7, Errors: 1')}

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}