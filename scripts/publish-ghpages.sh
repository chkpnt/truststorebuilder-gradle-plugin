#!/bin/bash -evu

openssl aes-256-cbc -k "$SSH_KEY_GITHUB_PASSPHRASE" -in .travis/github-deploy.key.enc -out .travis/github-deploy.key -d
chmod 600 .travis/github-deploy.key
eval $(ssh-agent -s)
ssh-add .travis/github-deploy.key

export GIT_AUTHOR_NAME=chkpnt CI
export GIT_AUTHOR_EMAIL=chkpnt-ci@chkpnt.de

./gradlew docs publishGhPages
