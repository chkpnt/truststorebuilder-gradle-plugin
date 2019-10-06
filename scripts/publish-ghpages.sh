#!/bin/bash -evu

openssl aes-256-cbc -K $encrypted_287f75608f39_key -iv $encrypted_287f75608f39_iv -in .travis/github-deploy.key.enc -out .travis/github-deploy.key -d
chmod 600 .travis/github-deploy.key
eval $(ssh-agent -s)
ssh-add .travis/github-deploy.key

export GIT_AUTHOR_NAME=chkpnt CI
export GIT_AUTHOR_EMAIL=chkpnt-ci@chkpnt.de

./gradlew docs publishGhPages
