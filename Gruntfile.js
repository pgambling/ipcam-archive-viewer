//
// NOTE: This is not doing any file optimization yet.
//

module.exports = function(grunt) {
    'use strict';

    // TODO
    //var BUILD_NUMBER = grunt.option('buildNumber') || grunt.template.date('isoUtcDateTime');

    // Project configuration.
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        // clean steps
        clean: {
            // remove all of the files in 00_publish
            pre: ['00_publish/', '*.zip', '*.tar.gz']

            // remove excess files leftover from build process
            //
            // TODO
            //
            // post: [
            //     // delete all js and css
            //     '00_publish/js/*',
            //     '00_publish/css/*',

            //     // except for these files
            //     '!00_publish/css/images/**',
            //     '!00_publish/css/font/**',
            //     '!00_publish/css/bootstrap-2.3.2.min.css',
            //     '!00_publish/css/main.css',
            //     '!00_publish/css/login.css',
            //     '!00_publish/js/app.js'
            // ]
        },

        // copy files to the 00_publish directory
        // NOTE: you have to explicitly add files and directories here
        //       think: whitelist
        copy: {
            publish: {
                files: {
                    '00_publish/' : [
                        'node_modules/express/**',
                        'node_modules/http-proxy/**',
                        'public/**',
                        '*.js',
                        '*.json',
                        '!config.json' // only include the example_config.json
                    ]
                }
            }
        },

        // wrap everything up into a zip package
        compress: {
            main: {
                options: {
                    mode: 'zip',
                    archive: '<%= pkg.name %>.zip'
                },
                cwd: '00_publish/',
                src: ['**/*'],
                dest: '.'
            }
        },

        exec: {
            addStartScript: {
                command: 'cp ../start.sh . && chmod +x start.sh',
                cwd: '00_publish'
            },
            tarball: {
                command: 'mv 00_publish <%= pkg.name %> && tar -czf <%= pkg.name %>.tar.gz <%= pkg.name %> && mv <%= pkg.name %> 00_publish'
            }
        },

        jshint: {
            options: {
                bitwise: true,
                camelcase: true,
                curly: false,
                eqeqeq: true,
                forin: false,
                immed: true,
                latedef: true,
                newcap: true,
                noarg: true,
                noempty: true,
                nonew: true,
                undef: true,
                unused: true,
                trailing: true,
                maxparams: 4,
                maxdepth: 3,
                maxlen: 150
            },
            serverFiles: {
                options: {
                    strict: false,
                    node: true
                },
                files: {
                    src: [
                        '*.js'
                    ]
                }
            },
            clientFiles: {
                options: {
                    strict: true,
                    browser: true,
                    jquery: true,
                    // add globals that jshint should know about here
                    globals: {
                        _: true,
                        GSO: true,
                        module: true,
                        Slick: true
                    }
                },
                files: {
                    src: [
                        'public/js/*.js',
                        'public/js/common/global.js',
                        'public/js/agency_manager/agency_manager.js',
                        // excluding third-party libs
                    ]
                }
            }
        },

        // tasks run automatically during development
        watch: {
            // lint the code anytime a js file changes
            js: {
                files: [
                    'js/**/*.js'
                ],
                tasks: ['jshint']
            }
        }

    });

    // Load tasks from npm
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    // grunt.loadNpmTasks('grunt-contrib-watch');
    // grunt.loadNpmTasks('grunt-contrib-cssmin');
    // grunt.loadNpmTasks('grunt-contrib-uglify');
    // grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-compress');
    //grunt.loadNpmTasks('grunt-lessless');
    grunt.loadNpmTasks('grunt-exec');

    // default task is to build a deployable package into the 00_publish folder
    grunt.registerTask('default',
        ['clean:pre', 'copy:publish', 'exec:addStartScript', 'exec:tarball']);

    // zip task creates a .zip file of the 00_publish folder
    grunt.registerTask('zip', ['default', 'compress']);
};
